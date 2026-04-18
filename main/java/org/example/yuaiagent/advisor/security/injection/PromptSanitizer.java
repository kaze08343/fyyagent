package org.example.yuaiagent.advisor.security.injection;

public class PromptSanitizer {

    /**
     * 清洗输入：
     * 1. 小写化
     * 2. 去除特殊符号（防止 i g n o r e 绕过）
     */
    public String normalize(String text) {
        if (text == null) return "";

        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", ""); // 保留中英文数字
    }
}