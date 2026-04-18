package org.example.yuaiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.ai.chat.messages.UserMessage;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {

    private static final String TOOL_RETRY_PROMPT = """
            You did not call any tool in the previous step.
            If the user's task is not fully completed, you must continue by calling the appropriate tool.
            If the task has already been fully completed, you must call the `terminate` tool to end the task.
            Do not stop without either using a tool or calling `terminate`.
            """;

    public abstract boolean think();

    public abstract String act();

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                if (getCurrentStep() >= getMaxSteps()) {
                    setState(org.example.yuaiagent.agent.model.AgentState.FINISHED);
                    return "思考完成 - 已达到最大重试次数";
                }

                getMessageList().add(new UserMessage(TOOL_RETRY_PROMPT));
                return "思考完成 - 本轮未调用工具，已要求下一轮必须继续调用工具或调用 terminate";
            }
            return act();
        } catch (Exception e) {
            e.printStackTrace();
            return "步骤执行失败: " + e.getMessage();
        }
    }
}
