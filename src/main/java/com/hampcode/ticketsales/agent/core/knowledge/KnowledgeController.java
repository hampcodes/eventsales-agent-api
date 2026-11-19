package com.hampcode.ticketsales.agent.core.knowledge;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/agent/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private static final int RESULTS = 3;

    private final VectorStore vectorStore;

    @GetMapping("/search")
    public List<KnowledgeMatch> search(@RequestParam String q) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(RESULTS).build()).stream()
                .map(document -> new KnowledgeMatch(
                        (String) document.getMetadata().get("file_name"),
                        document.getMetadata().get("page_number"),
                        document.getScore(),
                        document.getText()))
                .toList();
    }
}
