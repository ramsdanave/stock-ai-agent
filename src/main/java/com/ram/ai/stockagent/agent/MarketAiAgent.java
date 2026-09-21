package com.ram.ai.stockagent.agent;

import com.ram.ai.stockagent.rag.RagService;
import com.ram.ai.stockagent.tools.StockAnalysisTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MarketAiAgent {

    private final ChatClient chatClient;
    private final StockAnalysisTools stockAnalysisTools;
    private final RagService ragService;

    public MarketAiAgent(
            ChatClient.Builder chatClientBuilder,
            StockAnalysisTools stockAnalysisTools,
            RagService ragService) {

        this.chatClient = chatClientBuilder.build();
        this.stockAnalysisTools = stockAnalysisTools;
        this.ragService = ragService;
    }

    public String chat(String userMessage) {

        String systemPrompt = loadSystemPrompt();

        List<Document> documents =
                ragService.search(userMessage);

        String ragContext = documents.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n\n" + b);

        String finalPrompt = systemPrompt
                + "\n\nRETRIEVED KNOWLEDGE FROM RAG:\n"
                + ragContext
                + "\n\nUse the retrieved knowledge only when relevant. "
                + "Do not treat it as live market data.";

        return chatClient.prompt()
                .system(finalPrompt)
                .user(userMessage)
                .tools(stockAnalysisTools)
                .call()
                .content();
    }

    private String loadSystemPrompt() {

        try {

            ClassPathResource resource =
                    new ClassPathResource(
                            "prompts/market-ai-system.st"
                    );

            return new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to load MarketAI system prompt",
                    e
            );
        }
    }
}