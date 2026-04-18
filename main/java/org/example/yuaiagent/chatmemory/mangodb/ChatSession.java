//
//package org.example.yuaiagent.chatmemory.mangodb;
//
//import lombok.Data;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.mapping.Document;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Document(collection = "chat_sessions")
//@Data
//public class ChatSession {
//
//    @Id
//    private String sessionId;
//
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//
//    /**
//     * 全量消息（长期存储）
//     */
//    private List<ChatMessage> messages = new ArrayList<>();
//
//    /**
//     * 消息计数（用于触发summary）
//     */
//    private int messageCount = 0;
//
//    /**
//     * 阶段性摘要列表（每7条生成一个）
//     */
//    private List<String> summaries = new ArrayList<>();
//
//    /**
//     * 全局摘要（summary超过阈值后生成）
//     */
//    private String globalSummary;
//
//    /**
//     * 最近一次summary时间（可选，用于扩展）
//     */
//    private LocalDateTime lastSummaryAt;
//
//    // ================== 消息操作 ==================
//
//    public void addMessage(String role, String content) {
//        messages.add(new ChatMessage(role, content, LocalDateTime.now()));
//        messageCount++;
//        updatedAt = LocalDateTime.now();
//    }
//
//    public List<ChatMessage> getLastNMessages(int n) {
//        if (messages.size() <= n) {
//            return new ArrayList<>(messages);
//        }
//        return messages.subList(messages.size() - n, messages.size());
//    }
//
//    public void clearMessages() {
//        messages.clear();
//        messageCount = 0;
//        summaries.clear();
//        globalSummary = null;
//        updatedAt = LocalDateTime.now();
//    }
//
//    // ================== Summary操作 ==================
//
//    /**
//     * 添加阶段summary
//     */
//    public void addSummary(String summary) {
//        summaries.add(summary);
//        lastSummaryAt = LocalDateTime.now();
//    }
//
//    /**
//     * 获取所有summary
//     */
//    public List<String> getSummaries() {
//        return summaries;
//    }
//
//    /**
//     * 获取最近一个summary
//     */
//    public String getLatestSummary() {
//        if (summaries == null || summaries.isEmpty()) {
//            return null;
//        }
//        return summaries.get(summaries.size() - 1);
//    }
//
//    /**
//     * 获取最近N个summary（用于扩展）
//     */
//    public List<String> getRecentSummaries(int n) {
//        if (summaries.size() <= n) {
//            return new ArrayList<>(summaries);
//        }
//        return summaries.subList(summaries.size() - n, summaries.size());
//    }
//
//    /**
//     * 更新global summary
//     */
//    public void updateGlobalSummary(String globalSummary) {
//        this.globalSummary = globalSummary;
//    }
//
//    /**
//     * 是否需要触发阶段summary（每7条）
//     */
//    public boolean shouldCreateSummary(int threshold) {
//        return messageCount > 0 && messageCount % threshold == 0;
//    }
//
//    /**
//     * 是否需要生成global summary
//     */
//    public boolean shouldCreateGlobalSummary(int threshold) {
//        return summaries != null && summaries.size() > threshold;
//    }
//
//    // ================== 内部类 ==================
//
//    public record ChatMessage(String role, String content, LocalDateTime timestamp) {}
//}
package org.example.yuaiagent.chatmemory.mangodb;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "chat_sessions")
@Data
public class ChatSession {

    @Id
    private String sessionId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 全量消息（长期存储）
     */
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * 🔥 待总结消息池（避免重复总结）
     */
    private List<ChatMessage> pendingMessages = new ArrayList<>();

    /**
     * 阶段性摘要列表
     */
    private List<String> summaries = new ArrayList<>();

    /**
     * 🔥 summary token累计（优化性能）
     */
    private int summariesTokenCount = 0;

    /**
     * 全局用户画像
     */
    private String globalSummary;

    /**
     * 最近一次summary时间
     */
    private LocalDateTime lastSummaryAt;

    // ================== 消息操作 ==================

    public void addMessage(String role, String content) {
        ChatMessage msg = new ChatMessage(role, content, LocalDateTime.now());

        messages.add(msg);
        pendingMessages.add(msg); // 🔥 自动进入待总结池

        updatedAt = LocalDateTime.now();
    }

    public List<ChatMessage> getLastNMessages(int n) {
        if (messages.size() <= n) {
            return new ArrayList<>(messages);
        }
        return messages.subList(messages.size() - n, messages.size());
    }

    public void clearMessages() {
        messages.clear();
        pendingMessages.clear();
        summaries.clear();
        globalSummary = null;
        summariesTokenCount = 0;
        updatedAt = LocalDateTime.now();
    }

    // ================== Summary操作 ==================

    /**
     * 添加阶段summary（带token更新）
     */
    public void addSummary(String summary, int tokenCount) {
        summaries.add(summary);
        summariesTokenCount += tokenCount; // 🔥 累加token
        lastSummaryAt = LocalDateTime.now();
    }

    /**
     * 获取所有summary
     */
    public List<String> getSummaries() {
        return summaries;
    }

    /**
     * 获取最近一个summary
     */
    public String getLatestSummary() {
        if (summaries == null || summaries.isEmpty()) {
            return null;
        }
        return summaries.get(summaries.size() - 1);
    }

    /**
     * 获取最近N个summary
     */
    public List<String> getRecentSummaries(int n) {
        if (summaries.size() <= n) {
            return new ArrayList<>(summaries);
        }
        return summaries.subList(summaries.size() - n, summaries.size());
    }

    /**
     * 获取summary总token（O(1)）
     */
    public int getSummariesTokenCount() {
        return summariesTokenCount;
    }

    /**
     * 更新用户画像
     */
    public void updateGlobalSummary(String globalSummary) {
        this.globalSummary = globalSummary;
    }

    /**
     * 清空pending（summary完成后调用）
     */
    public void clearPendingMessages() {
        pendingMessages.clear();
    }

    // ================== 内部类 ==================

    public record ChatMessage(String role, String content, LocalDateTime timestamp) {}
}