package com.hampcode.ticketsales.agent.core.knowledge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeLoaderTest {

    @Mock VectorStore vectorStore;
    @Mock JdbcTemplate jdbcTemplate;

    KnowledgeLoader knowledgeLoader;

    @BeforeEach
    void setUp() {
        knowledgeLoader = new KnowledgeLoader(vectorStore, jdbcTemplate, TokenTextSplitter.builder().build());
    }

    @Test
    void loadsPdfAsChunksWithFileNameAndHashWhenItIsNew() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);

        knowledgeLoader.run(null);

        ArgumentCaptor<List<Document>> chunks = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(chunks.capture());
        assertFalse(chunks.getValue().isEmpty());
        chunks.getValue().forEach(chunk -> {
            assertEquals("politicas-eventsales.pdf", chunk.getMetadata().get("file_name"));
            assertTrue(chunk.getMetadata().containsKey("source_hash"));
            assertTrue(chunk.getMetadata().containsKey("page_number"));
        });
    }

    @Test
    void deletesThePreviousVersionBeforeLoadingAChangedPdf() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);

        knowledgeLoader.run(null);

        verify(jdbcTemplate).update(anyString(), eq("politicas-eventsales.pdf"));
    }

    @Test
    void doesNothingWhenTheSamePdfIsAlreadyLoaded() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(12);

        knowledgeLoader.run(null);

        verify(vectorStore, never()).add(any());
        verify(jdbcTemplate, never()).update(anyString(), any(Object.class));
    }
}
