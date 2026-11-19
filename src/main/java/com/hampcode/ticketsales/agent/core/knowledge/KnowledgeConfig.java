package com.hampcode.ticketsales.agent.core.knowledge;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeConfig {

    private static final int CHUNK_SIZE_TOKENS = 800;
    private static final int TOP_K = 4;
    private static final double SIMILARITY_THRESHOLD = 0.3;

    @Bean
    TokenTextSplitter textSplitter() {
        return TokenTextSplitter.builder().withChunkSize(CHUNK_SIZE_TOKENS).build();
    }

    @Bean
    QuestionAnswerAdvisor knowledgeAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(TOP_K).similarityThreshold(SIMILARITY_THRESHOLD).build())
                .build();
    }
}
