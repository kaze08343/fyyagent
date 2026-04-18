package org.example.yuaiagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.yuaiagent.chatmemory.mangodb.ChatSessionRepository;
import org.example.yuaiagent.chatmemory.mangodb.LongShortTermMemoryService;
import org.example.yuaiagent.chatmemory.mangodb.MongoChatMemory;
import org.example.yuaiagent.skill.SkillEvolutionService;
import org.example.yuaiagent.skill.SkillRegistry;
import org.example.yuaiagent.skill.SkillSelector;
import org.example.yuaiagent.tools.JsonToolExecutor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class YuManus extends ToolCallAgent {

    public YuManus(ToolCallback[] allTools,
                   ChatModel dashscopeChatModel,
                   List<JsonToolExecutor> jsonToolExecutors,
                   ChatSessionRepository chatSessionRepository,
                   LongShortTermMemoryService longShortTermMemoryService,
                   SkillSelector skillSelector,
                   SkillEvolutionService skillEvolutionService,
                   SkillRegistry skillRegistry,
                   VectorStore loveAppVectorStore) {
        super(allTools, dashscopeChatModel, jsonToolExecutors,
                buildChatMemory(chatSessionRepository, longShortTermMemoryService),
                skillSelector,
                skillEvolutionService,
                skillRegistry);
        setName("yuManus");

        String SYSTEM_PROMPT = """
                You are YuManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.

                IMPORTANT BEHAVIOR RULES:
                1. Think step by step and use tools when needed.
                2. For tasks requiring search, file operations, terminal operations, or PDF generation, use the appropriate tool instead of only describing what to do.
                3. When the user's request has been fully completed, you MUST call the `terminate` tool to end the task.
                4. Do not stop silently after tool execution. If the task is complete, call `terminate`.
                5. Keep each step concise and action-oriented.
                """;
        setSystemPrompt(SYSTEM_PROMPT);

        String NEXT_STEP_PROMPT = """
                Decide the single best next action.
                - If a tool is needed, call exactly the appropriate tool.
                - After each successful tool execution, evaluate whether the overall task is complete.
                - If the task is complete, call the `terminate` tool immediately.
                - Only avoid tools when you are giving the final direct answer for a simple request.
                """;
        setNextStepPrompt(NEXT_STEP_PROMPT);
        setMaxSteps(10);
    }

    private static ChatMemory buildChatMemory(ChatSessionRepository chatSessionRepository,
                                              LongShortTermMemoryService longShortTermMemoryService) {
        return new MongoChatMemory(chatSessionRepository, longShortTermMemoryService);
    }
}
