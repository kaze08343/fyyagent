package org.example.yuaiagent.advisor.security.injection;

public class PromptGuardService {

    private final PromptSanitizer sanitizer = new PromptSanitizer();
    private final InjectionRuleEngine ruleEngine = new InjectionRuleEngine();
    private final InjectionSemanticGuard semanticGuard;

    public PromptGuardService(InjectionSemanticGuard semanticGuard) {
        this.semanticGuard = semanticGuard;
    }

    public void check(String userInput) {

        String normalized = sanitizer.normalize(userInput);

        // 1️⃣ 规则检测
        if (ruleEngine.match(userInput, normalized)) {
            throw new RuntimeException("检测到潜在恶意输入（规则匹配）");
        }

        // 2️⃣ 语义检测（可开关）
        if (semanticGuard != null && semanticGuard.isInjection(userInput)) {
            throw new RuntimeException("检测到潜在恶意输入（语义检测）");
        }
    }
}