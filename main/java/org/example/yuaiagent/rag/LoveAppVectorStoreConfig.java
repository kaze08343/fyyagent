package org.example.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;
    @Resource
    private MyKeywordEnricher myKeywordEnricher;
    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();

        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();

        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documents);
        simpleVectorStore.add(enrichedDocuments);
        return simpleVectorStore;
    }
}
