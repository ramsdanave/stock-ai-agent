package com.ram.ai.stockagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/search")
    public List<String> search(@RequestParam String query) {

        return ragService.search(query)
                .stream()
                .map(Document::getText)
                .toList();
    }
}