//////package org.example.yuaiagent.chatmemory.mangodb;
//////
//////import jakarta.annotation.Resource;
//////import lombok.extern.slf4j.Slf4j;
//////import org.springframework.ai.chat.client.ChatClient;
//////import org.springframework.ai.chat.model.ChatModel;
//////import org.springframework.stereotype.Service;
//////
//////import java.time.LocalDateTime;
//////import java.util.List;
//////import java.util.Map;
//////import java.util.concurrent.ConcurrentHashMap;
//////
//////@Service
//////@Slf4j
//////public class LongShortTermMemoryService {
//////
//////    private static final int SHORT_TERM_MEMORY_SIZE = 2;
//////    private static final int SUMMARY_TRIGGER_THRESHOLD = 5;
//////
//////    private final ChatSessionRepository repository;
//////    private final ChatModel chatModel;
//////
//////    private final Map<String, ShortTermMemory> shortTermMemoryCache = new ConcurrentHashMap<>();
//////
//////    public LongShortTermMemoryService(ChatSessionRepository repository, ChatModel chatModel) {
//////        this.repository = repository;
//////        this.chatModel = chatModel;
//////    }
//////
//////    public void addMessage(String conversationId, String role, String content) {
//////        ChatSession session = getOrCreateSession(conversationId);
//////        session.addMessage(role, content);
//////
//////        updateShortTermMemory(conversationId, role, content);
//////
//////        if (session.shouldCreateSummary(SUMMARY_TRIGGER_THRESHOLD)) {
//////            generateAndUpdateSummary(session);
//////        }
//////
//////        repository.save(session);
//////        log.debug("添加消息到会话: {}, 角色: {}, 总消息数: {}", conversationId, role, session.getMessageCount());
//////    }
//////
//////    public List<ChatSession.ChatMessage> getRecentMessages(String conversationId, int limit) {
//////        ShortTermMemory shortTerm = shortTermMemoryCache.get(conversationId);
//////        if (shortTerm != null && !shortTerm.isEmpty()) {
//////            return shortTerm.getMessages(Math.min(limit, SHORT_TERM_MEMORY_SIZE));
//////        }
//////
//////        return repository.findBySessionId(conversationId)
//////                .map(session -> session.getLastNMessages(limit))
//////                .orElse(List.of());
//////    }
//////
//////    public String getSessionSummary(String conversationId) {
//////        return repository.findBySessionId(conversationId)
//////                .map(ChatSession::getSessionSummary)
//////                .orElse(null);
//////    }
//////
//////    public void clearShortTermMemory(String conversationId) {
//////        shortTermMemoryCache.remove(conversationId);
//////        log.info("清除短期记忆: {}", conversationId);
//////    }
//////
//////    public void clearAllMemory(String conversationId) {
//////        shortTermMemoryCache.remove(conversationId);
//////        repository.findBySessionId(conversationId).ifPresent(session -> {
//////            session.clearMessages();
//////            session.updateSummary(null);
//////            repository.save(session);
//////        });
//////        log.info("清除所有记忆: {}", conversationId);
//////    }
//////
//////    private ChatSession getOrCreateSession(String conversationId) {
//////        return repository.findBySessionId(conversationId)
//////                .orElseGet(() -> {
//////                    ChatSession session = new ChatSession();
//////                    session.setSessionId(conversationId);
//////                    session.setCreatedAt(LocalDateTime.now());
//////                    session.setUpdatedAt(LocalDateTime.now());
//////                    return session;
//////                });
//////    }
//////
//////    private void updateShortTermMemory(String conversationId, String role, String content) {
//////        shortTermMemoryCache.computeIfAbsent(conversationId, k -> new ShortTermMemory(SHORT_TERM_MEMORY_SIZE))
//////                .addMessage(role, content);
//////    }
//////
//////    private void generateAndUpdateSummary(ChatSession session) {
//////        try {
//////            String summaryPrompt = buildSummaryPrompt(session);
//////
//////            ChatClient chatClient = ChatClient.builder(chatModel).build();
//////            String summary = chatClient.prompt(summaryPrompt)
//////                    .call()
//////                    .content();
//////
//////            session.updateSummary(summary);
//////            log.info("生成会话摘要成功: {}, 摘要长度: {}", session.getSessionId(), summary.length());
//////        } catch (Exception e) {
//////            log.error("生成会话摘要失败: {}", session.getSessionId(), e);
//////        }
//////    }
//////
//////    private String buildSummaryPrompt(ChatSession session) {
//////        StringBuilder prompt = new StringBuilder();
//////        prompt.append("请根据以下对话历史生成一个简洁的摘要（200字以内），包含关键信息和用户偏好：\n\n");
//////
//////        List<ChatSession.ChatMessage> recentMessages = session.getLastNMessages(30);
//////        for (ChatSession.ChatMessage msg : recentMessages) {
//////            prompt.append(msg.role()).append(": ").append(msg.content()).append("\n");
//////        }
//////
//////        prompt.append("\n如果之前有摘要，请结合新内容更新摘要。\n");
//////        if (session.getSessionSummary() != null) {
//////            prompt.append("之前的摘要：").append(session.getSessionSummary()).append("\n");
//////        }
//////
//////        prompt.append("\n请输出摘要：");
//////        return prompt.toString();
//////    }
//////
//////    private static class ShortTermMemory {
//////        private final int maxSize;
//////        private final List<ChatSession.ChatMessage> messages;
//////
//////        public ShortTermMemory(int maxSize) {
//////            this.maxSize = maxSize;
//////            this.messages = new java.util.ArrayList<>();
//////        }
//////
//////        public synchronized void addMessage(String role, String content) {
//////            messages.add(new ChatSession.ChatMessage(role, content, LocalDateTime.now()));
//////            if (messages.size() > maxSize) {
//////                messages.remove(0);
//////            }
//////        }
//////
//////        public synchronized List<ChatSession.ChatMessage> getMessages(int limit) {
//////            if (messages.size() <= limit) {
//////                return new java.util.ArrayList<>(messages);
//////            }
//////            return messages.subList(messages.size() - limit, messages.size());
//////        }
//////
//////        public synchronized boolean isEmpty() {
//////            return messages.isEmpty();
//////        }
//////    }
//////}
////package org.example.yuaiagent.chatmemory.mangodb;
////
////import jakarta.annotation.Resource;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.ai.chat.client.ChatClient;
////import org.springframework.ai.chat.model.ChatModel;
////import org.springframework.ai.ollama.OllamaChatModel;
////import org.springframework.beans.factory.annotation.Qualifier;
////import org.springframework.stereotype.Service;
////
////import java.time.LocalDateTime;
////import java.util.List;
////import java.util.Map;
////import java.util.concurrent.ConcurrentHashMap;
////
////@Service
////@Slf4j
////public class LongShortTermMemoryService {
////
////    private static final int SHORT_TERM_MEMORY_SIZE = 2;
////    private static final int SUMMARY_TRIGGER_THRESHOLD = 5;
////
////    private final ChatSessionRepository repository;
////    private final ChatModel dashscopeChatModel;
////    private final OllamaChatModel ollamaChatModel;
////
////    private final Map<String, ShortTermMemory> shortTermMemoryCache = new ConcurrentHashMap<>();
////
////    public LongShortTermMemoryService(ChatSessionRepository repository,
////                                      @Qualifier("dashscopeChatModel") ChatModel dashscopeChatModel,
////                                      OllamaChatModel ollamaChatModel) {
////        this.repository = repository;
////        this.dashscopeChatModel = dashscopeChatModel;
////        this.ollamaChatModel = ollamaChatModel;
////        log.info("长期记忆总结模型已初始化 - Ollama Model: {}", ollamaChatModel.getDefaultOptions().getModel());
////    }
////
////    public void addMessage(String conversationId, String role, String content) {
////        ChatSession session = getOrCreateSession(conversationId);
////        session.addMessage(role, content);
////
////        updateShortTermMemory(conversationId, role, content);
////
////        if (session.shouldCreateSummary(SUMMARY_TRIGGER_THRESHOLD)) {
////            generateAndUpdateSummary(session);
////        }
////
////        repository.save(session);
////        log.debug("添加消息到会话: {}, 角色: {}, 总消息数: {}", conversationId, role, session.getMessageCount());
////    }
////
////    public List<ChatSession.ChatMessage> getRecentMessages(String conversationId, int limit) {
////        ShortTermMemory shortTerm = shortTermMemoryCache.get(conversationId);
////        if (shortTerm != null && !shortTerm.isEmpty()) {
////            return shortTerm.getMessages(Math.min(limit, SHORT_TERM_MEMORY_SIZE));
////        }
////
////        return repository.findBySessionId(conversationId)
////                .map(session -> session.getLastNMessages(limit))
////                .orElse(List.of());
////    }
////
////    public String getSessionSummary(String conversationId) {
////        return repository.findBySessionId(conversationId)
////                .map(ChatSession::getSessionSummary)
////                .orElse(null);
////    }
////
////    public void clearShortTermMemory(String conversationId) {
////        shortTermMemoryCache.remove(conversationId);
////        log.info("清除短期记忆: {}", conversationId);
////    }
////
////    public void clearAllMemory(String conversationId) {
////        shortTermMemoryCache.remove(conversationId);
////        repository.findBySessionId(conversationId).ifPresent(session -> {
////            session.clearMessages();
////            session.updateSummary(null);
////            repository.save(session);
////        });
////        log.info("清除所有记忆: {}", conversationId);
////    }
////
////    private ChatSession getOrCreateSession(String conversationId) {
////        return repository.findBySessionId(conversationId)
////                .orElseGet(() -> {
////                    ChatSession session = new ChatSession();
////                    session.setSessionId(conversationId);
////                    session.setCreatedAt(LocalDateTime.now());
////                    session.setUpdatedAt(LocalDateTime.now());
////                    return session;
////                });
////    }
////
////    private void updateShortTermMemory(String conversationId, String role, String content) {
////        shortTermMemoryCache.computeIfAbsent(conversationId, k -> new ShortTermMemory(SHORT_TERM_MEMORY_SIZE))
////                .addMessage(role, content);
////    }
////
////    private void generateAndUpdateSummary(ChatSession session) {
////        try {
////            String summaryPrompt = buildSummaryPrompt(session);
////
////            ChatClient chatClient = ChatClient.builder(ollamaChatModel).build();
////            String summary = chatClient.prompt(summaryPrompt)
////                    .call()
////                    .content();
////
////            session.updateSummary(summary);
////            log.info("生成会话摘要成功: {}, 摘要长度: {}", session.getSessionId(), summary.length());
////        } catch (Exception e) {
////            log.error("生成会话摘要失败: {}", session.getSessionId(), e);
////        }
////    }
////
////    private String buildSummaryPrompt(ChatSession session) {
////        StringBuilder prompt = new StringBuilder();
////        prompt.append("请根据以下对话历史生成一个简洁的摘要（200字以内），包含关键信息和用户偏好：\n\n");
////
////        List<ChatSession.ChatMessage> recentMessages = session.getLastNMessages(30);
////        for (ChatSession.ChatMessage msg : recentMessages) {
////            prompt.append(msg.role()).append(": ").append(msg.content()).append("\n");
////        }
////
////        prompt.append("\n如果之前有摘要，请结合新内容更新摘要。\n");
////        if (session.getSessionSummary() != null) {
////            prompt.append("之前的摘要：").append(session.getSessionSummary()).append("\n");
////        }
////
////        prompt.append("\n请输出摘要：");
////        return prompt.toString();
////    }
////
////    private static class ShortTermMemory {
////        private final int maxSize;
////        private final List<ChatSession.ChatMessage> messages;
////
////        public ShortTermMemory(int maxSize) {
////            this.maxSize = maxSize;
////            this.messages = new java.util.ArrayList<>();
////        }
////
////        public synchronized void addMessage(String role, String content) {
////            messages.add(new ChatSession.ChatMessage(role, content, LocalDateTime.now()));
////            if (messages.size() > maxSize) {
////                messages.remove(0);
////            }
////        }
////
////        public synchronized List<ChatSession.ChatMessage> getMessages(int limit) {
////            if (messages.size() <= limit) {
////                return new java.util.ArrayList<>(messages);
////            }
////            return messages.subList(messages.size() - limit, messages.size());
////        }
////
////        public synchronized boolean isEmpty() {
////            return messages.isEmpty();
////        }
////    }
////}
//package org.example.yuaiagent.chatmemory.mangodb;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.ollama.OllamaChatModel;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//@Slf4j
//public class LongShortTermMemoryService {
//
//    private static final int SHORT_TERM_MEMORY_SIZE = 20;
//    private static final int SUMMARY_TRIGGER_THRESHOLD = 7;
//    private static final int GLOBAL_SUMMARY_THRESHOLD = 5;
//
//    private final ChatSessionRepository repository;
//    private final OllamaChatModel ollamaChatModel;
//
//    private final Map<String, ShortTermMemory> shortTermMemoryCache = new ConcurrentHashMap<>();
//
//    public LongShortTermMemoryService(ChatSessionRepository repository,
//                                      OllamaChatModel ollamaChatModel) {
//        this.repository = repository;
//        this.ollamaChatModel = ollamaChatModel;
//    }
//
//    // ================== 对话入口 ==================
//    public void addMessage(String conversationId, String role, String content) {
//        ChatSession session = getOrCreateSession(conversationId);
//        session.addMessage(role, content);
//
//        updateShortTermMemory(conversationId, role, content);
//
//        // 每7条触发一次summary
//        if (session.getMessageCount() % SUMMARY_TRIGGER_THRESHOLD == 0) {
//            generateIncrementalSummary(session);
//        }
//
//        repository.save(session);
//    }
//
//    // ================== 构造最终Prompt ==================
//    public String buildFinalPrompt(String conversationId, String userInput) {
//
//        ChatSession session = getOrCreateSession(conversationId);
//
//        StringBuilder prompt = new StringBuilder();
//
//        // ✅ 只有global存在才拼接
//        if (session.getGlobalSummary() != null) {
//            prompt.append("【用户长期背景】\n")
//                    .append(session.getGlobalSummary())
//                    .append("\n\n");
//        }
//
//        List<ChatSession.ChatMessage> recent =
//                getRecentMessages(conversationId, SHORT_TERM_MEMORY_SIZE);
//
//        prompt.append("【最近对话】\n");
//        for (ChatSession.ChatMessage msg : recent) {
//            prompt.append(msg.role())
//                    .append(": ")
//                    .append(msg.content())
//                    .append("\n");
//        }
//
//        prompt.append("\n【用户问题】\n")
//                .append(userInput);
//
//        return prompt.toString();
//    }
//
//    // ================== 增量Summary ==================
//    private void generateIncrementalSummary(ChatSession session) {
//        try {
//            List<ChatSession.ChatMessage> lastMessages =
//                    session.getLastNMessages(SUMMARY_TRIGGER_THRESHOLD);
//
//            String lastSummary = session.getLatestSummary();
//
//            String prompt = buildIncrementalSummaryPrompt(lastSummary, lastMessages);
//
//            ChatClient chatClient = ChatClient.builder(ollamaChatModel).build();
//            String newSummary = chatClient.prompt(prompt).call().content();
//
//            session.addSummary(newSummary);
//
//            log.info("阶段摘要生成成功: {}", newSummary);
//
//            // 触发global summary
//            if (session.getSummaries().size() > GLOBAL_SUMMARY_THRESHOLD) {
//                generateGlobalSummary(session);
//            }
//
//        } catch (Exception e) {
//            log.error("阶段摘要生成失败", e);
//        }
//    }
//
//    // ================== Summary Prompt ==================
//    private String buildIncrementalSummaryPrompt(String lastSummary,
//                                                 List<ChatSession.ChatMessage> newMessages) {
//
//        StringBuilder prompt = new StringBuilder();
//
//        prompt.append("请根据以下内容更新摘要（200字以内）：\n\n");
//
//        if (lastSummary != null) {
//            prompt.append("【已有摘要】\n")
//                    .append(lastSummary)
//                    .append("\n\n");
//        }
//
//        prompt.append("【新增对话】\n");
//
//        for (ChatSession.ChatMessage msg : newMessages) {
//            prompt.append(msg.role())
//                    .append(": ")
//                    .append(msg.content())
//                    .append("\n");
//        }
//
//        prompt.append("\n要求：\n")
//                .append("1. 保留重要长期信息（用户背景、目标）\n")
//                .append("2. 用新增内容更新摘要\n")
//                .append("3. 删除重复内容\n")
//                .append("4. 删除过时信息\n")
//                .append("5. 输出完整新摘要");
//
//        return prompt.toString();
//    }
//
//    // ================== Global Summary ==================
//    private void generateGlobalSummary(ChatSession session) {
//        try {
//            List<String> summaries = session.getSummaries();
//
//            StringBuilder prompt = new StringBuilder();
//            prompt.append("请将以下多个摘要压缩为一个全局摘要（200字以内）：\n\n");
//
//            for (String s : summaries) {
//                prompt.append(s).append("\n");
//            }
//
//            ChatClient chatClient = ChatClient.builder(ollamaChatModel).build();
//            String global = chatClient.prompt(prompt.toString()).call().content();
//
//            session.updateGlobalSummary(global);
//
//            log.info("全局摘要生成成功");
//
//        } catch (Exception e) {
//            log.error("全局摘要生成失败", e);
//        }
//    }
//
//    // ================== ShortTerm ==================
//    private void updateShortTermMemory(String conversationId, String role, String content) {
//        shortTermMemoryCache
//                .computeIfAbsent(conversationId,
//                        k -> new ShortTermMemory(SHORT_TERM_MEMORY_SIZE))
//                .addMessage(role, content);
//    }
//
//    public List<ChatSession.ChatMessage> getRecentMessages(String conversationId, int limit) {
//        ShortTermMemory shortTerm = shortTermMemoryCache.get(conversationId);
//        if (shortTerm != null && !shortTerm.isEmpty()) {
//            return shortTerm.getMessages(limit);
//        }
//
//        return repository.findBySessionId(conversationId)
//                .map(session -> session.getLastNMessages(limit))
//                .orElse(List.of());
//    }
//
//    // ================== Session ==================
//    private ChatSession getOrCreateSession(String conversationId) {
//        return repository.findBySessionId(conversationId)
//                .orElseGet(() -> {
//                    ChatSession session = new ChatSession();
//                    session.setSessionId(conversationId);
//                    session.setCreatedAt(LocalDateTime.now());
//                    session.setUpdatedAt(LocalDateTime.now());
//                    return session;
//                });
//    }
//
//    // ================== ShortTerm内部类 ==================
//    private static class ShortTermMemory {
//        private final int maxSize;
//        private final List<ChatSession.ChatMessage> messages = new ArrayList<>();
//
//        public ShortTermMemory(int maxSize) {
//            this.maxSize = maxSize;
//        }
//
//        public synchronized void addMessage(String role, String content) {
//            messages.add(new ChatSession.ChatMessage(role, content, LocalDateTime.now()));
//            if (messages.size() > maxSize) {
//                messages.remove(0); // FIFO
//            }
//        }
//
//        public synchronized List<ChatSession.ChatMessage> getMessages(int limit) {
//            if (messages.size() <= limit) {
//                return new ArrayList<>(messages);
//            }
//            return messages.subList(messages.size() - limit, messages.size());
//        }
//
//        public synchronized boolean isEmpty() {
//            return messages.isEmpty();
//        }
//    }
//}
package org.example.yuaiagent.chatmemory.mangodb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LongShortTermMemoryService {

    private static final int SHORT_TERM_MEMORY_SIZE = 15;

    private static final int SUMMARY_TRIGGER_TOKENS = 2000;
    private static final int SUMMARY_MAX_TOKENS = 200;
    private static final int GLOBAL_SUMMARY_TRIGGER_TOKENS = 1000;

    private final ChatSessionRepository repository;
    private final ChatModel dashscopeChatModel;

    private final Map<String, ShortTermMemory> shortTermMemoryCache = new ConcurrentHashMap<>();

    public LongShortTermMemoryService(ChatSessionRepository repository,
                                      @Qualifier("dashscopeChatModel") ChatModel dashscopeChatModel) {
        this.repository = repository;
        this.dashscopeChatModel = dashscopeChatModel;
        log.info("长期记忆总结模型已初始化 - DashScope Model");
    }

    public void addMessage(String conversationId, String role, String content) {

        ChatSession session = getOrCreateSession(conversationId);

        ChatSession.ChatMessage message =
                new ChatSession.ChatMessage(role, content, LocalDateTime.now());

        session.addMessage(role, content);

        updateShortTermMemory(conversationId, role, content);

        if (estimateMessagesTokens(session.getPendingMessages()) >= SUMMARY_TRIGGER_TOKENS) {
            generateIncrementalSummary(session);
            session.clearPendingMessages();
        }

        repository.save(session);
    }

    public String buildFinalPrompt(String conversationId, String userInput) {

        ChatSession session = getOrCreateSession(conversationId);

        StringBuilder prompt = new StringBuilder();

        if (session.getGlobalSummary() != null) {
            prompt.append("【用户长期画像】\n")
                    .append(session.getGlobalSummary())
                    .append("\n\n");
        }

        List<ChatSession.ChatMessage> recent =
                getRecentMessages(conversationId, SHORT_TERM_MEMORY_SIZE);

        prompt.append("【最近对话】\n");

        for (ChatSession.ChatMessage msg : recent) {
            prompt.append(msg.role())
                    .append(": ")
                    .append(msg.content())
                    .append("\n");
        }

        prompt.append("\n【用户问题】\n")
                .append(userInput);

        return prompt.toString();
    }

    public List<ChatSession.ChatMessage> getRecentMessages(String conversationId, int limit) {

        ShortTermMemory memory = shortTermMemoryCache.get(conversationId);

        if (memory != null && !memory.isEmpty()) {
            return memory.getMessages(limit);
        }

        List<ChatSession.ChatMessage> messages = repository.findBySessionId(conversationId)
                .map(session -> session.getLastNMessages(limit))
                .orElse(List.of());

        if (!messages.isEmpty()) {
            ShortTermMemory newMemory = new ShortTermMemory(SHORT_TERM_MEMORY_SIZE);

            for (ChatSession.ChatMessage msg : messages) {
                newMemory.addMessage(msg.role(), msg.content());
            }

            shortTermMemoryCache.put(conversationId, newMemory);

            log.info("短期记忆从Mongo恢复成功: {}", conversationId);
        }

        return messages;
    }

    private void updateShortTermMemory(String conversationId, String role, String content) {

        ShortTermMemory memory = shortTermMemoryCache.computeIfAbsent(conversationId, k -> {

            List<ChatSession.ChatMessage> messages = repository.findBySessionId(conversationId)
                    .map(session -> session.getLastNMessages(SHORT_TERM_MEMORY_SIZE))
                    .orElse(List.of());

            ShortTermMemory m = new ShortTermMemory(SHORT_TERM_MEMORY_SIZE);

            for (ChatSession.ChatMessage msg : messages) {
                m.addMessage(msg.role(), msg.content());
            }

            return m;
        });

        memory.addMessage(role, content);
    }

    private void generateIncrementalSummary(ChatSession session) {
        try {

            List<ChatSession.ChatMessage> newMessages = session.getPendingMessages();
            String lastSummary = session.getLatestSummary();

            String prompt = buildIncrementalSummaryPrompt(lastSummary, newMessages);

            ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();

            String newSummary = chatClient.prompt(prompt).call().content();
            int tokens = estimateTokens(newSummary);
            session.addSummary(newSummary, tokens);
            session.clearPendingMessages();


            log.info("阶段摘要生成成功");

            if (estimateSummariesTokens(session.getSummaries()) >= GLOBAL_SUMMARY_TRIGGER_TOKENS) {
                generateGlobalSummary(session);
            }

        } catch (Exception e) {
            log.error("阶段摘要生成失败", e);
        }
    }

    private String buildIncrementalSummaryPrompt(String lastSummary,
                                                 List<ChatSession.ChatMessage> newMessages) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("请对以下对话生成摘要（不超过200 tokens）：\n\n");

        if (lastSummary != null) {
            prompt.append("【已有摘要】\n")
                    .append(lastSummary)
                    .append("\n\n");
        }

        prompt.append("【新增对话】\n");

        for (ChatSession.ChatMessage msg : newMessages) {
            prompt.append(msg.role())
                    .append(": ")
                    .append(msg.content())
                    .append("\n");
        }

        prompt.append("\n要求：保留关键信息，避免重复");

        return prompt.toString();
    }

    private void generateGlobalSummary(ChatSession session) {
        try {

            List<String> summaries = session.getSummaries();

            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下摘要提取用户画像（技术栈/兴趣/偏好）：\n\n");

            for (String s : summaries) {
                prompt.append(s).append("\n");
            }

            prompt.append("\n要求：去重，结构化");

            ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();

            String global = chatClient.prompt(prompt.toString()).call().content();

            session.updateGlobalSummary(global);

            log.info("用户画像生成成功");

        } catch (Exception e) {
            log.error("全局摘要生成失败", e);
        }
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        return (int) (text.length() / 1.2);
    }

    private int estimateMessagesTokens(List<ChatSession.ChatMessage> messages) {
        return messages.stream()
                .mapToInt(m -> estimateTokens(m.content()))
                .sum();
    }

    private int estimateSummariesTokens(List<String> summaries) {
        return summaries.stream()
                .mapToInt(this::estimateTokens)
                .sum();
    }

    private ChatSession getOrCreateSession(String conversationId) {
        return repository.findBySessionId(conversationId)
                .orElseGet(() -> {
                    ChatSession session = new ChatSession();
                    session.setSessionId(conversationId);
                    session.setCreatedAt(LocalDateTime.now());
                    session.setUpdatedAt(LocalDateTime.now());
                    return session;
                });
    }

    private static class ShortTermMemory {
        private final int maxSize;
        private final List<ChatSession.ChatMessage> messages = new ArrayList<>();

        public ShortTermMemory(int maxSize) {
            this.maxSize = maxSize;
        }

        public synchronized void addMessage(String role, String content) {
            messages.add(new ChatSession.ChatMessage(role, content, LocalDateTime.now()));
            if (messages.size() > maxSize) {
                messages.remove(0);
            }
        }

        public synchronized List<ChatSession.ChatMessage> getMessages(int limit) {
            if (messages.size() <= limit) {
                return new ArrayList<>(messages);
            }
            return messages.subList(messages.size() - limit, messages.size());
        }

        public synchronized boolean isEmpty() {
            return messages.isEmpty();
        }
    }
}
