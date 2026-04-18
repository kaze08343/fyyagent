package org.example.yuaiagent.tools;

import cn.hutool.json.JSONObject;

public interface JsonToolExecutor {

    String toolName();

    String execute(JSONObject arguments);
}
