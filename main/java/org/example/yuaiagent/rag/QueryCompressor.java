package org.example.yuaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class QueryCompressor {

    private final QueryTransformer compressQueryTransformer;

    public QueryCompressor(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        
        // Spring AI 自带的查询压缩转换器
        this.compressQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /**
     * 压缩用户查询
     * @param prompt 原始查询
     * @return 压缩后的查询
     */
    public String compress(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return prompt;
        }
        
        Query query = new Query(prompt);
        Query compressedQuery = compressQueryTransformer.transform(query);
        
        log.info("压缩查询：{} -> {}", prompt, compressedQuery.text());
        return compressedQuery.text();
    }

    /**
     * 智能压缩：短查询不压缩，长查询才压缩
     * @param prompt 原始查询
     * @return 压缩后的查询
     */
    public String smartCompress(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return prompt;
        }
        
        // 如果查询本身就很短（< 15 字），不需要压缩
        if (prompt.length() < 15) {
            log.debug("查询较短，无需压缩：{}", prompt);
            return prompt;
        }
        
        return compress(prompt);
    }
}
