package org.example.yuaiagent.tools;

import cn.hutool.json.JSONObject;

public class WebSearchToolExecutor implements JsonToolExecutor {

    private final WebSearchTool webSearchTool;

    public WebSearchToolExecutor(WebSearchTool webSearchTool) {
        this.webSearchTool = webSearchTool;
    }

    @Override
    public String toolName() {
        return "searchWeb";
    }

    @Override
    public String execute(JSONObject arguments) {
        String query = arguments == null ? "" : arguments.getStr("query", "");
        return webSearchTool.searchWeb(query);
    }
}
