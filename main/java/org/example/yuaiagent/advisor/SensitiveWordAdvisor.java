

//
//package org.example.yuaiagent.advisor;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.advisor.api.*;
//import reactor.core.publisher.Flux;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Slf4j
//public class SensitiveWordAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {
//
//    private final Set<String> sensitiveWords;
//
//    public SensitiveWordAdvisor() {
//        this.sensitiveWords = new HashSet<>(Arrays.asList(
//                "敏感词 1",
//                "敏感词 2",
//                "违禁词 1"
//        ));
//    }
//
//    @Override
//    public String getName() {
//        return this.getClass().getSimpleName();
//    }
//
//    @Override
//    public int getOrder() {
//        return -1; // 在其他 advisor 之前执行，优先拦截
//    }
//
//    private AdvisedRequest before(AdvisedRequest request) {
//        String userText = request.userText();
//
//        if (containsSensitiveWord(userText)) {
//            log.warn("检测到敏感词，已拦截请求：{}", userText);
//            throw new IllegalArgumentException("您的输入包含不当内容，请修改后重试");
//        }
//
//        log.info("内容检查通过：{}", userText);
//        return request;
//    }
//
//    private boolean containsSensitiveWord(String text) {
//        if (text == null || text.isEmpty()) {
//            return false;
//        }
//
//        for (String word : sensitiveWords) {
//            if (text.contains(word)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
//        advisedRequest = this.before(advisedRequest);
//        return chain.nextAroundCall(advisedRequest);
//    }
//
//    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
//        advisedRequest = this.before(advisedRequest);
//        return chain.nextAroundStream(advisedRequest);
//    }
//}
//
//
//
//
//




package org.example.yuaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.example.yuaiagent.advisor.dfa.SensitiveWordFilter;
import org.example.yuaiagent.advisor.security.BanService;
import org.example.yuaiagent.advisor.security.PromptInjectionDetector;
import org.springframework.ai.chat.client.advisor.api.*;
        import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
public class SensitiveWordAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final SensitiveWordFilter filter = new SensitiveWordFilter();
    private final BanService banService = new BanService();

    public SensitiveWordAdvisor() {
        // 初始化敏感词（可改为Redis加载）
        filter.loadWords(Set.of("敏感词1", "敏感词2", "违禁词"));
    }

    @Override
    public String getName() {
        return "SensitiveWordAdvisor";
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private AdvisedRequest before(AdvisedRequest request, String userId) {
        String userText = request.userText();

        // 🚫 1. 封禁检测
        if (banService.isBanned(userId)) {
            throw new RuntimeException("用户已被封禁，请稍后再试");
        }



        // 🔍 3. 敏感词检测（DFA）
        List<String> words = filter.match(userText);

        if (!words.isEmpty()) {
            banService.record(userId, words.size());

            int count = words.size();

            if (count >= 3) {
                log.error("严重违规: {}", words);
                throw new RuntimeException("严重违规内容");
            }

            if (count == 2) {
                log.warn("中度违规: {}", words);
                throw new IllegalArgumentException("内容不当，请修改");
            }

            if (count == 1) {
                log.info("轻度违规，自动替换: {}", words);
                String newText = filter.replace(userText);

                return AdvisedRequest.from(request)
                        .userText(newText)
                        .build();
            }
        }

        return request;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        String userId = extractUserId(request);
        request = before(request, userId);
        return chain.nextAroundCall(request);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        String userId = extractUserId(request);
        request = before(request, userId);
        return chain.nextAroundStream(request);
    }

    private String extractUserId(AdvisedRequest request) {
        // TODO: 实际应从JWT / Header获取
        return "anonymous";
    }
}









