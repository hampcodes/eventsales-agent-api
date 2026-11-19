package com.hampcode.ticketsales.agent.core.knowledge;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KnowledgeLoader implements ApplicationRunner {

    private static final String PDF_LOCATION = "classpath:knowledge/*.pdf";
    private static final String HASH_KEY = "source_hash";

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final TokenTextSplitter textSplitter;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (Resource pdf : new PathMatchingResourcePatternResolver().getResources(PDF_LOCATION)) {
            String fileName = pdf.getFilename();
            String hash = hashOf(pdf);
            if (isLoaded(fileName, hash)) continue;

            deleteFragmentsOf(fileName);
            List<Document> pages = extract(pdf);
            List<Document> chunks = transform(pages, hash);
            load(chunks);
        }
    }

    private List<Document> extract(Resource pdf) {
        return new PagePdfDocumentReader(pdf).get();
    }

    private List<Document> transform(List<Document> pages, String hash) {
        return textSplitter.apply(pages).stream()
                .map(chunk -> chunk.mutate().metadata(HASH_KEY, hash).build())
                .toList();
    }

    private void load(List<Document> chunks) {
        vectorStore.add(chunks);
    }

    private boolean isLoaded(String fileName, String hash) {
        Integer fragments = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vector_store WHERE metadata->>'file_name' = ? AND metadata->>'source_hash' = ?",
                Integer.class, fileName, hash);
        return fragments != null && fragments > 0;
    }

    private void deleteFragmentsOf(String fileName) {
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'file_name' = ?", fileName);
    }

    private static String hashOf(Resource pdf) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf.getContentAsByteArray()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
