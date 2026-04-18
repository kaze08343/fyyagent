package org.example.yuaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.example.yuaiagent.advisor.security.injection.PromptGuardService;
import org.example.yuaiagent.advisor.security.injection.PromptGuardService;
import org.example.yuaiagent.advisor.security.prompt.PromptTemplateBuilder;
import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

@Slf4j
public class InjectionGuardAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final PromptGuardService guardService;

    public InjectionGuardAdvisor(PromptGuardService guardService) {
        this.guardService = guardService;
    }

    @Override
    public String getName() {
        return "InjectionGuardAdvisor";
    }

    @Override
    public int getOrder() {
        return -10; // 比敏感词更早执行
    }

    private AdvisedRequest before(AdvisedRequest request) {

        String userInput = request.userText();

        // 🚫 1. 检测攻击
        guardService.check(userInput);

        // 🔒 2. Prompt隔离
        String safePrompt = PromptTemplateBuilder.buildSafePrompt(
                "你是专业医疗助手",
                userInput
        );

        return AdvisedRequest.from(request)
                .userText(safePrompt)
                .build();
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        request = before(request);
        return chain.nextAroundCall(request);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        request = before(request);
        return chain.nextAroundStream(request);
    }
}