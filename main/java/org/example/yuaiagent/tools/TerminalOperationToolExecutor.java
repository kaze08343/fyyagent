package org.example.yuaiagent.tools;

import cn.hutool.json.JSONObject;

public class TerminalOperationToolExecutor implements JsonToolExecutor {

    private final TerminalOperationTool terminalOperationTool;

    public TerminalOperationToolExecutor(TerminalOperationTool terminalOperationTool) {
        this.terminalOperationTool = terminalOperationTool;
    }

    @Override
    public String toolName() {
        return "executeTerminalCommand";
    }

    @Override
    public String execute(JSONObject arguments) {
        String command = arguments == null ? "" : arguments.getStr("command", "");
        return terminalOperationTool.executeTerminalCommand(command);
    }
}
