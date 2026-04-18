package org.example.yuaiagent.advisor.security;

import java.util.List;

public class PromptInjectionDetector {

    private static final List<String> PATTERNS = List.of(
            "忽略之前的指令",
            "你现在是",
            "系统提示",
            "system prompt",
            "act as",
            "ignore previous instructions",
            "developer mode"
    );

    public boolean isInjection(String text) {
        String lower = text.toLowerCase();

        for (String p : PATTERNS) {
            if (lower.contains(p.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}