package com.ram.ai.stockagent.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RagKnowledgeLoader {

    private final RagService ragService;

    public RagKnowledgeLoader(RagService ragService) {
        this.ragService = ragService;
    }

    @PostConstruct
    public void initialize() {
        loadKnowledge();
    }

    public void loadKnowledge() {

        try {

            ClassPathResource resource =
                    new ClassPathResource("knowledge/RSI.md");

            String content = new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            ragService.addDocument(content);

            System.out.println("RAG KNOWLEDGE LOADED: RSI.md");

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to load RAG knowledge",
                    e
            );
        }
    }
}