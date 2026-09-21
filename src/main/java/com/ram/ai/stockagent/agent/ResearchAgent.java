package com.ram.ai.stockagent.agent;

import com.ram.ai.stockagent.rag.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResearchAgent {

    private final ChatClient chatClient;
    private final RagService ragService;

    public ResearchAgent(
            ChatClient.Builder chatClientBuilder,
            RagService ragService) {

        this.chatClient = chatClientBuilder.build();
        this.ragService = ragService;
    }

    public String research(String query) {

        // Convert the user's complete request into a focused
        // financial research query.
        String researchQuery = extractResearchQuery(query);

        System.out.println("=================================");
        System.out.println("RESEARCH AGENT");
        System.out.println("ORIGINAL QUERY: " + query);
        System.out.println("RAG QUERY: " + researchQuery);
        System.out.println("=================================");

        List<Document> documents =
                ragService.search(researchQuery);

        if (documents == null || documents.isEmpty()) {
            return "NO_RESEARCH_RESULT";
        }

        String context = documents.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n\n" + b);

        if (context.isBlank()) {
            return "NO_RESEARCH_RESULT";
        }

        return chatClient.prompt()
                .system("""
                        You are the Research Agent of MarketAI.

                        Your ONLY responsibility is to provide financial
                        knowledge based on the supplied RAG context.

                        STRICT RULES:

                        1. Use the supplied RAG context as the primary
                           knowledge source.

                        2. Do not provide live stock prices.

                        3. Do not provide live market data.

                        4. Do not invent financial information.

                        5. Do not make guaranteed investment predictions.

                        6. Do not add unrelated company information.

                        7. If the RAG context is relevant, explain it
                           clearly and concisely.

                        8. If the RAG context is not relevant to the
                           research query, return exactly:

                           NO_RESEARCH_RESULT

                        Return concise factual research that another
                        MarketAI agent can use.
                        """)
                .user("""
                        RESEARCH QUERY:
                        %s

                        RAG CONTEXT:
                        %s
                        """.formatted(researchQuery, context))
                .call()
                .content();
    }

    private String extractResearchQuery(String query) {

        String result = chatClient.prompt()
                .system("""
                        You are a research-query extraction component
                        inside MarketAI.

                        Extract ONLY the financial concept that should
                        be searched in the RAG knowledge base.

                        Examples:

                        User:
                        Analyze TCS technically and explain what its RSI means

                        Output:
                        RSI

                        User:
                        What does RSI below 30 mean?

                        Output:
                        RSI

                        User:
                        Explain MACD and analyze INFY

                        Output:
                        MACD

                        User:
                        What is a moving average?

                        Output:
                        moving average

                        Return ONLY the concept or topic.
                        Do not provide an explanation.
                        """)
                .user(query)
                .call()
                .content()
                .trim();

        if (result.isBlank()) {
            return query;
        }

        return result;
    }
}