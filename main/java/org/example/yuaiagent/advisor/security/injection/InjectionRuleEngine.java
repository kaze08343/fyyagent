package org.example.yuaiagent.advisor.security.injection;

import java.util.List;
import java.util.regex.Pattern;

public class InjectionRuleEngine {

    // 关键词规则
    private static final List<String> KEYWORDS = List.of(
            "忽略之前的指令",
            "你现在是",
            "系统提示",
            "systemprompt",
            "actas",
            "ignorepreviousinstructions",
            "developermode"
    );

    // 正则规则（更强）
    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("ignore\\s*previous\\s*instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act\\s*as\\s*.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("你现在是.*"),
            Pattern.compile("忽略.*指令")
    );

    public boolean match(String rawText, String normalizedText) {

        // 1️⃣ 关键词检测（清洗后）
        for (String k : KEYWORDS) {
            if (normalizedText.contains(k)) {
                return true;
            }
        }

        // 2️⃣ 正则检测（原文本）
        for (Pattern p : PATTERNS) {
            if (p.matcher(rawText).find()) {
                return true;
            }
        }

        return false;
    }
}