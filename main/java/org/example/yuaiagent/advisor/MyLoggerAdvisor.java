package org.example.yuaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

@Slf4j
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private AdvisedRequest before(AdvisedRequest request) {
        log.info("=== AI Request ===");
        log.info("用户输入: {}", request.userText());
        if (request.systemText() != null && !request.systemText().isEmpty()) {
            log.debug("系统提示: {}", request.systemText().substring(0, Math.min(100, request.systemText().length())));
        }
        return request;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {
        AssistantMessage message = advisedResponse.response().getResult().getOutput();
        log.info("=== AI Response ===");
        log.info("回复内容: {}", message.getText());

        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
            log.info("工具调用数量: {}", message.getToolCalls().size());
            message.getToolCalls().forEach(toolCall -> {
                log.info("  - 工具名: {}, 参数: {}", toolCall.name(), toolCall.arguments());
            });
        } else {
            log.info("本轮无需调用工具（可能是最终回复或工具已自动执行完毕）");
        }
    }

    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        advisedRequest = this.before(advisedRequest);
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        this.observeAfter(advisedResponse);
        return advisedResponse;
    }

    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        advisedRequest = this.before(advisedRequest);
        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);
        return (new MessageAggregator()).aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }
}