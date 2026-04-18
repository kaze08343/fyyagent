package org.example.yuaiagent.tools;

import cn.hutool.json.JSONObject;

public class TerminateToolExecutor implements JsonToolExecutor {

    private final TerminateTool terminateTool;

    public TerminateToolExecutor(TerminateTool terminateTool) {
        this.terminateTool = terminateTool;
    }

    @Override
    public String toolName() {
        return "terminate";
    }

    @Override
    public String execute(JSONObject arguments) {
        return terminateTool.terminate();
    }
}
