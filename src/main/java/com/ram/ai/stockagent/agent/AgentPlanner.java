package com.ram.ai.stockagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AgentPlanner {

    private final ChatClient chatClient;

    public AgentPlanner(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public AgentPlan createPlan(String userMessage) {

        String json = chatClient.prompt()

                .system("""
                        You are the planning component of MarketAI.

                        Analyze the user's request and create an execution plan.

                        ============================================
                        SUPPORTED INTENTS
                        ============================================

                        RESEARCH

                        TECHNICAL_ANALYSIS

                        TECHNICAL_ANALYSIS_WITH_EXPLANATION

                        STOCK_COMPARISON

                        STOCK_RECOMMENDATION

                        LIVE_QUOTE

                        INVESTMENT_ALLOCATION


                        ============================================
                        SUPPORTED TASKS
                        ============================================

                        RAG_RESEARCH

                        LIVE_QUOTE

                        LIVE_ANALYSIS

                        STOCK_COMPARISON

                        STOCK_RECOMMENDATION

                        INVESTMENT_ALLOCATION


                        ============================================
                        GENERAL RULES
                        ============================================

                        1. Use RAG_RESEARCH for financial concepts
                           such as RSI, MACD, moving averages,
                           volume and technical-analysis concepts.

                        2. Use LIVE_QUOTE for current stock prices.

                        3. Use LIVE_ANALYSIS for technical analysis
                           of a specific stock.

                        4. Use STOCK_COMPARISON when the user asks
                           to compare multiple stocks.

                        5. Use STOCK_RECOMMENDATION for stock discovery
                           or sector-based stock recommendations.


                        ============================================
                        INVESTMENT ALLOCATION RULES
                        ============================================

                        6. Use INVESTMENT_ALLOCATION when the user
                           provides an investment amount and asks for
                           stocks in a specific sector.

                        7. For EVERY INVESTMENT_ALLOCATION request,
                           ALWAYS include BOTH of these tasks:

                           "STOCK_RECOMMENDATION"

                           "INVESTMENT_ALLOCATION"

                        8. Extract the investment amount.

                        9. Extract the sector.

                        10. Extract the requested number of stocks
                            when explicitly provided.

                        11. If the user does not specify a number
                            of stocks, use 5.

                        12. The selected stocks must come from the
                            existing STOCK_RECOMMENDATION engine.

                        13. Do not select stocks based on market cap
                            unless the user explicitly asks for it.

                        14. Do not select stocks based on valuation
                            unless the user explicitly asks for it.

                        15. Stock selection should use the application's
                            existing technical/performance ranking.

                        16. Stocks may be large-cap, mid-cap or
                            small-cap.

                        17. The requested investment amount can be
                            any positive amount.


                        ============================================
                        AMOUNT EXTRACTION
                        ============================================

                        Convert investment amounts into numeric values.

                        Examples:

                        ₹50,000 -> 50000

                        50k -> 50000

                        1 lakh -> 100000

                        2L -> 200000

                        1.5 lakh -> 150000

                        25 thousand -> 25000


                        ============================================
                        STOCK COUNT
                        ============================================

                        If the user explicitly asks:

                        "top 3 IT stocks"

                        stockCount = 3

                        If the user asks:

                        "top 10 IT stocks"

                        stockCount = 10

                        If the user does not specify the number:

                        stockCount = 5


                        ============================================
                        SECTOR NORMALIZATION
                        ============================================

                        Normalize common sector names.

                        Information Technology -> IT

                        Technology -> IT

                        Tech -> IT

                        Banking -> BANKING

                        Bank -> BANKING

                        Pharmaceutical -> PHARMA

                        Pharmaceuticals -> PHARMA

                        Pharma -> PHARMA

                        Automobile -> AUTO

                        Automobiles -> AUTO

                        Metals -> METAL

                        Metal -> METAL

                        Real Estate -> REALTY

                        Finance -> FINANCE


                        ============================================
                        SYMBOL RULES
                        ============================================

                        Extract the stock symbol when clearly present.

                        Extract multiple symbols when the user asks
                        for a comparison.

                        Do not invent stock symbols.

                        If there is no specific stock symbol,
                        return null for symbol.

                        If there are no comparison symbols,
                        return an empty array for symbols.


                        ============================================
                        IMPORTANT
                        ============================================

                        For:

                        "I have 50000 for IT sector"

                        return an investment allocation plan.

                        Expected interpretation:

                        intent = INVESTMENT_ALLOCATION

                        investmentAmount = 50000

                        sector = IT

                        stockCount = 5

                        tasks must contain BOTH:

                        STOCK_RECOMMENDATION

                        INVESTMENT_ALLOCATION


                        For:

                        "I have 50000 for top 3 IT stocks"

                        expected:

                        investmentAmount = 50000

                        sector = IT

                        stockCount = 3

                        tasks must contain BOTH:

                        STOCK_RECOMMENDATION

                        INVESTMENT_ALLOCATION


                        ============================================
                        JSON OUTPUT
                        ============================================

                        Return ONLY valid JSON.

                        Do not return markdown.

                        Do not return ```json.

                        Do not add explanations.

                        JSON format:

                        {
                          "intent": "INTENT",
                          "symbol": null,
                          "symbols": [],
                          "tasks": [],
                          "investmentAmount": null,
                          "sector": null,
                          "stockCount": 5
                        }
                        """)

                .user(userMessage)

                .call()

                .content();

        System.out.println(
                "AGENT PLANNER RAW RESPONSE: " + json
        );

        try {

            return new ObjectMapper()
                    .readValue(json, AgentPlan.class);

        } catch (Exception e) {

            System.err.println(
                    "AGENT PLAN PARSING FAILED: "
                            + e.getMessage()
            );

            AgentPlan fallback = new AgentPlan();

            fallback.setIntent("TECHNICAL_ANALYSIS");

            fallback.setTasks(
                    java.util.List.of("LIVE_ANALYSIS")
            );

            return fallback;
        }
    }
}