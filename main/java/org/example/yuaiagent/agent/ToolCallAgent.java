package org.example.yuaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.example.yuaiagent.agent.model.AgentState;
import org.example.yuaiagent.tools.JsonToolExecutor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import org.example.yuaiagent.skill.SkillContext;
import org.example.yuaiagent.skill.SkillEvolutionService;
import org.example.yuaiagent.skill.SkillRegistry;
import org.example.yuaiagent.skill.SkillSelector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ToolCallAgent（JSON 计划版）
 * think(): 只输出工具计划 JSON
 * act():   手动执行工具
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private static final int MEMORY_RETRIEVE_SIZE = 20;

    private static final int SKILL_MAX_LOAD = 2;

    private final ToolCallback[] availableTools;
    private final ChatModel chatModel;
    private final Map<String, JsonToolExecutor> toolExecutorRegistry;
    private final ChatMemory chatMemory;

    private final org.example.yuaiagent.skill.SkillSelector skillSelector;
    private final org.example.yuaiagent.skill.SkillEvolutionService skillEvolutionService;
    private final org.example.yuaiagent.skill.SkillRegistry skillRegistry;

    private ToolExecutionPlan pendingPlan;
    private int persistedMessageCount = 0;

    private String currentTaskKey;

    public ToolCallAgent(ToolCallback[] availableTools,
                         ChatModel chatModel,
                         List<JsonToolExecutor> toolExecutors,
                         ChatMemory chatMemory,
                         org.example.yuaiagent.skill.SkillSelector skillSelector,
                         org.example.yuaiagent.skill.SkillEvolutionService skillEvolutionService,
                         org.example.yuaiagent.skill.SkillRegistry skillRegistry) {
        this.availableTools = availableTools;
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.skillSelector = skillSelector;
        this.skillEvolutionService = skillEvolutionService;
        this.skillRegistry = skillRegistry;
        this.toolExecutorRegistry = toolExecutors.stream().collect(Collectors.toMap(
                JsonToolExecutor::toolName,
                executor -> executor,
                (left, right) -> left,
                LinkedHashMap::new
        ));

        log.info("=== 注册工具 ===");
        for (ToolCallback tool : availableTools) {
            log.info("工具: {} | {}", tool.getToolDefinition().name(), tool.getToolDefinition().description());
        }
        log.info("================\n");
    }

    public record PlannedToolCall(String name, JSONObject arguments) {}

    public record ToolExecutionPlan(boolean needTools,
                                    String finalAnswer,
                                    String taskKey,
                                    String useSkill,
                                    String skillName,
                                    String skillContent,
                                    List<PlannedToolCall> toolCalls) {}

    @Override
    public boolean think() {
        String plannerPrompt = buildPlannerPrompt();

        try {
            syncNewMessagesToMemory();

            // Build skill context for this user prompt
            org.example.yuaiagent.skill.SkillContext skillContext = (skillSelector == null)
                    ? new org.example.yuaiagent.skill.SkillContext(java.util.List.of(), java.util.List.of())
                    : skillSelector.buildContextForPrompt(getLatestUserPrompt(), SKILL_MAX_LOAD);

            List<Message> messages = new ArrayList<>();
            if (StrUtil.isNotBlank(getSystemPrompt())) {
                messages.add(new SystemMessage(getSystemPrompt()));
            }
            messages.addAll(loadConversationMessages());

            if (!skillContext.index().isEmpty()) {
                messages.add(new SystemMessage(buildSkillIndexSystemMessage(skillContext)));
            }
            if (!skillContext.loaded().isEmpty()) {
                messages.add(new SystemMessage(buildLoadedSkillsSystemMessage(skillContext)));
            }

            messages.add(new UserMessage(plannerPrompt));

            ChatResponse chatResponse = chatModel.call(new Prompt(messages));
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String content = assistantMessage.getText();

            log.info("\n========== {} THINK ==========" , getName());
            log.info("思考内容: {}", content);

            ToolExecutionPlan plan = parsePlan(content);
            this.pendingPlan = plan;
            this.currentTaskKey = plan == null ? null : plan.taskKey();

            if (plan == null) {
                log.info("工具调用数量: 0");
                getMessageList().add(new AssistantMessage("计划解析失败: " + content));
                syncNewMessagesToMemory();
                log.info("================================\n");
                return false;
            }

            log.info("needTools: {}", plan.needTools());
            log.info("工具调用数量: {}", plan.toolCalls().size());
            plan.toolCalls().forEach(tc -> log.info("工具: {} | 参数: {}", tc.name(), tc.arguments()));
            log.info("================================\n");

            // Skill evolution: model proposes a new/updated skillContent
            if ("create".equalsIgnoreCase(plan.useSkill()) || "update".equalsIgnoreCase(plan.useSkill())) {
                if (isValidSkillName(plan.skillName()) && looksLikeSkillMd(plan.skillContent())) {
                    skillRegistry.upsertSkill(plan.skillName(), plan.skillContent());
                    if (StrUtil.isNotBlank(plan.taskKey())) {
                        skillEvolutionService.bindSkillName(plan.taskKey(), plan.skillName());
                    }
                    getMessageList().add(new AssistantMessage("Skill saved: " + plan.skillName()));
                    syncNewMessagesToMemory();
                } else {
                    getMessageList().add(new AssistantMessage("skill_save_failed: invalid skillName or skillContent"));
                    syncNewMessagesToMemory();
                }
            }

            if (!plan.needTools()) {
                if (StrUtil.isNotBlank(plan.finalAnswer())) {
                    getMessageList().add(new AssistantMessage(plan.finalAnswer()));
                    syncNewMessagesToMemory();
                    setState(AgentState.FINISHED);
                }
                return false;
            }

            return CollUtil.isNotEmpty(plan.toolCalls());
        } catch (Exception e) {
            log.error("THINK ERROR: {}", e.getMessage(), e);
            getMessageList().add(new AssistantMessage("处理失败: " + e.getMessage()));
            syncNewMessagesToMemory();
            return false;
        }
    }

    @Override
    public String act() {
        if (pendingPlan == null || CollUtil.isEmpty(pendingPlan.toolCalls())) {
            return "没有工具需要调用";
        }

        try {
            log.info("\n========== {} ACT ==========" , getName());

            List<String> resultLines = new ArrayList<>();
            List<String> structuredResults = new ArrayList<>();
            for (PlannedToolCall toolCall : pendingPlan.toolCalls()) {
                String result = executeOneTool(toolCall);
                resultLines.add("工具[" + toolCall.name() + "] -> " + result);
                structuredResults.add(buildStructuredToolResult(toolCall, result));

                if ("terminate".equals(toolCall.name())) {
                    log.info("检测到 terminate，结束任务");
                    setState(AgentState.FINISHED);
                }
            }

        String output = String.join("\n", resultLines);
        String structuredOutput = String.join("\n", structuredResults);
        getMessageList().add(new AssistantMessage(structuredOutput));
        syncNewMessagesToMemory();

        // update task usage stats (for evolution trigger)
        if (skillEvolutionService != null) {
            String key = this.currentTaskKey;
            if (StrUtil.isBlank(key)) {
                key = skillEvolutionService.recordTaskAndGetTaskKey(getLatestUserPrompt());
            }
            long count = skillEvolutionService.incrementUsage(key);
            // hint the model when threshold reached and no skill is bound
            boolean hasBound = skillEvolutionService.getBoundSkillName(key).isPresent();
            if (skillEvolutionService.shouldEvolve(count, hasBound)) {
                getMessageList().add(new AssistantMessage("skill_evolve_hint: threshold_reached taskKey=" + key + " count=" + count));
                syncNewMessagesToMemory();
            }
        }

        log.info("执行结果:\n{}", output);
        log.info("================================\n");
        return output;
        } catch (Exception e) {
            log.error("ACT ERROR: {}", e.getMessage(), e);
            return "工具执行异常: " + e.getMessage();
        }
    }

    private void syncNewMessagesToMemory() {
        if (chatMemory == null || StrUtil.isBlank(getCurrentChatId())) {
            return;
        }
        List<Message> messageList = getMessageList();
        if (messageList.size() <= persistedMessageCount) {
            return;
        }
        List<Message> newMessages = new ArrayList<>(messageList.subList(persistedMessageCount, messageList.size()));
        chatMemory.add(getCurrentChatId(), newMessages);
        persistedMessageCount = messageList.size();
    }

    private List<Message> loadConversationMessages() {
        if (chatMemory != null && StrUtil.isNotBlank(getCurrentChatId())) {
            return new ArrayList<>(chatMemory.get(getCurrentChatId(), MEMORY_RETRIEVE_SIZE));
        }
        return new ArrayList<>(getMessageList());
    }

    private String executeOneTool(PlannedToolCall toolCall) {
        JsonToolExecutor executor = toolExecutorRegistry.get(toolCall.name());
        if (executor == null) {
            return "未知工具: " + toolCall.name();
        }
        JSONObject args = toolCall.arguments() == null ? new JSONObject() : toolCall.arguments();
        return executor.execute(args);
    }

    private String buildStructuredToolResult(PlannedToolCall toolCall, String result) {
        JSONObject payload = new JSONObject();
        payload.set("type", "tool_result");
        payload.set("tool", toolCall.name());
        payload.set("arguments", toolCall.arguments() == null ? new JSONObject() : toolCall.arguments());
        payload.set("status", isSuccessResult(result) ? "success" : "failure");
        payload.set("result", result);
        return payload.toString();
    }

    private boolean isSuccessResult(String result) {
        if (result == null) {
            return false;
        }
        String normalized = result.toLowerCase();
        return !normalized.contains("error") && !normalized.contains("failed") && !normalized.contains("异常");
    }

    private ToolExecutionPlan parsePlan(String rawContent) {
        try {
            String jsonText = stripMarkdownCodeFence(rawContent);
            JSONObject root = JSONUtil.parseObj(jsonText);
            boolean needTools = root.getBool("needTools", false);
            String finalAnswer = root.getStr("finalAnswer", "");
            String taskKey = root.getStr("taskKey", "");
            String useSkill = root.getStr("useSkill", "");
            String skillName = root.getStr("skillName", "");
            String skillContent = root.getStr("skillContent", "");

            JSONArray toolCallsArray = root.getJSONArray("toolCalls");
            List<PlannedToolCall> toolCalls = new ArrayList<>();

            if (toolCallsArray != null) {
                for (Object item : toolCallsArray) {
                    JSONObject obj = JSONUtil.parseObj(item);
                    String name = obj.getStr("name", "");
                    JSONObject arguments = obj.getJSONObject("arguments");
                    toolCalls.add(new PlannedToolCall(name, arguments));
                }
            }
            return new ToolExecutionPlan(needTools, finalAnswer, taskKey, useSkill, skillName, skillContent, toolCalls);
        } catch (Exception e) {
            log.warn("解析 JSON 计划失败: {}", e.getMessage());
            return null;
        }
    }

    private String stripMarkdownCodeFence(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String getLatestUserPrompt() {
        List<Message> messages = getMessageList();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            if (m instanceof UserMessage) {
                return ((UserMessage) m).getText();
            }
        }
        return "";
    }

    private String buildPlannerPrompt() {
        String toolDescriptions = java.util.Arrays.stream(availableTools)
                .map(tool -> String.format("- %s: %s", tool.getToolDefinition().name(), tool.getToolDefinition().description()))
                .collect(Collectors.joining("\n"));

        return """
                You are in THINK mode only.
                Do NOT execute any tool.
                Decide the next action and return ONLY valid JSON.

                Available tools:
                %s

                JSON schema:
                {
                  "needTools": true,
                  "finalAnswer": "",
                  "taskKey": "",
                  "useSkill": "none", // one of: none | follow | create | update
                  "skillName": "",     // required when useSkill is create/update
                  "skillContent": "",  // required when useSkill is create/update, must be SKILL.md full text
                  "toolCalls": [
                    {
                      "name": "tool_name",
                      "arguments": {
                        "arg1": "value"
                      }
                    }
                  ]
                }

                Rules:
                1. For task-based requests, do not directly claim the task is complete unless you include the needed tool calls.
                2. When the task has been fully completed, the next plan must contain only one tool call: `terminate`.
                3. Do not repeat the same tool if the previous execution of that tool already succeeded and completed that step.
                4. Only retry the same tool when the previous tool result clearly failed or was insufficient.
                5. Review prior tool_result messages before planning the next action.
                6. Skill usage: If loaded skills match the user's request, set useSkill="follow" and follow the skill instructions.
                7. Skill evolution: If the same task repeats (>=3) or user adds new constraints for an existing task, set useSkill to create/update and output valid SKILL.md in skillContent.
                   SKILL.md MUST start with YAML frontmatter and include `name:` and `description:`.
                   Example format:
                   ---
                   name: jinan-travel-pdf
                   description: Generates a Jinan travel guidance PDF. Use when user asks for Jinan weather/travel PDF.
                   ---
                   # Jinan travel PDF
                   
                   ## Instructions
                   - Step 1: ...
                   - Step 2: ...
                   
                   ## Examples
                   ...
                8. If you create/update a skill, choose a stable taskKey for the task and set it in taskKey.
                9. For simple non-tool questions, set needTools=false and put the answer in finalAnswer.
                10. Output JSON only. No markdown, no explanation.
                """.formatted(toolDescriptions);
    }


    private String buildSkillIndexSystemMessage(SkillContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Available skills index (project ./skill):\n");
        for (var e : context.index()) {
            sb.append("- ").append(e.name()).append(": ").append(e.description()).append("\n");
        }
        sb.append("\nIf a skill matches the request, prefer using it. If user adds new constraints, update the skill.");
        return sb.toString();
    }

    private String buildLoadedSkillsSystemMessage(SkillContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Loaded skills (follow these if relevant):\n\n");
        for (var s : context.loaded()) {
            sb.append("==== SKILL: ").append(s.name()).append(" ====\n");
            sb.append(s.content()).append("\n\n");
        }
        return sb.toString();
    }

    private boolean isValidSkillName(String name) {
        return StrUtil.isNotBlank(name) && name.matches("^[a-z0-9-]{1,64}$");
    }

    private boolean looksLikeSkillMd(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }
        String t = content.trim();
        if (!t.startsWith("---")) {
            return false;
        }
        return t.contains("\nname:") && t.contains("\ndescription:") && t.contains("---");
    }

}
