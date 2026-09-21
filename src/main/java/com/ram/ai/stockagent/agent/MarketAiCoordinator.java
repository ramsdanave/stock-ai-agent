package com.ram.ai.stockagent.agent;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ArrayNode;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.ram.ai.stockagent.tools.StockAnalysisTools;

import org.springframework.ai.chat.client.ChatClient;

import org.springframework.stereotype.Service;

@Service

public class MarketAiCoordinator {

    private final ChatClient chatClient;

    private final StockAnalysisTools stockAnalysisTools;

    private final AgentPlanner agentPlanner;

    private final ResearchAgent researchAgent;

    private final AnalysisAgent analysisAgent;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarketAiCoordinator(

            ChatClient.Builder chatClientBuilder,

            AgentPlanner agentPlanner,

            ResearchAgent researchAgent,

            AnalysisAgent analysisAgent,

            StockAnalysisTools stockAnalysisTools) {

        this.chatClient = chatClientBuilder.build();

        this.agentPlanner = agentPlanner;

        this.researchAgent = researchAgent;

        this.analysisAgent = analysisAgent;

        this.stockAnalysisTools = stockAnalysisTools;

    }

    public String process(String userMessage) {

        // ============================================

        // 1. CREATE AGENT PLAN

        // ============================================

        AgentPlan plan = agentPlanner.createPlan(userMessage);

        System.out.println("=================================");

        System.out.println("MARKETAI AGENT PLANNER");

        System.out.println("USER QUERY: " + userMessage);

        System.out.println("INTENT: " + plan.getIntent());

        System.out.println("SYMBOL: " + plan.getSymbol());

        System.out.println("SYMBOLS: " + plan.getSymbols());

        System.out.println("TASKS: " + plan.getTasks());

        System.out.println("INVESTMENT AMOUNT: " + plan.getInvestmentAmount());

        System.out.println("SECTOR: " + plan.getSector());

        System.out.println("STOCK COUNT: " + plan.getStockCount());

        System.out.println("=================================");

        // ============================================

        // 2. INITIALIZE RESULTS

        // ============================================

        String researchResult = "NOT_REQUESTED";

        String analysisResult = "NOT_REQUESTED";

        String comparisonResult = "NOT_REQUESTED";

        String liveQuoteResult = "NOT_REQUESTED";

        String recommendationResult = "NOT_REQUESTED";

        // ============================================

        // 3. RAG RESEARCH

        // ============================================

        if (plan.getTasks() != null

                && plan.getTasks().contains("RAG_RESEARCH")) {

            researchResult = researchAgent.research(userMessage);

            System.out.println("RESEARCH AGENT EXECUTED");

            System.out.println("RESEARCH RESULT: " + researchResult);

        }

        // ============================================

        // 4. LIVE TECHNICAL ANALYSIS

        // ============================================

        if (plan.getTasks() != null

                && plan.getTasks().contains("LIVE_ANALYSIS")) {

            analysisResult = analysisAgent.analyze(userMessage);

            System.out.println("ANALYSIS AGENT EXECUTED");

            System.out.println("ANALYSIS RESULT: " + analysisResult);

        }

        // ============================================

        // 5. STOCK COMPARISON

        // ============================================

        if (plan.getTasks() != null

                && plan.getTasks().contains("STOCK_COMPARISON")) {

            if (plan.getSymbols() == null

                    || plan.getSymbols().isEmpty()) {

                comparisonResult = "NO_COMPARISON_RESULT";

            } else {

                try {

                    var results =

                            stockAnalysisTools.compareStocksOnline(

                                    plan.getSymbols()

                            );

                    if (results == null || results.isEmpty()) {

                        comparisonResult =

                                "NO_COMPARISON_RESULT";

                    } else {

                        comparisonResult =

                                objectMapper.writeValueAsString(results);

                    }

                } catch (Exception e) {

                    System.err.println(

                            "STOCK COMPARISON FAILED: "

                                    + e.getMessage()

                    );

                    comparisonResult =

                            "NO_COMPARISON_RESULT";

                }

            }

            System.out.println(

                    "COMPARISON AGENT/TOOL EXECUTED"

            );

            System.out.println(

                    "COMPARISON RESULT: "

                            + comparisonResult

            );

        }

        // ============================================

        // 6. LIVE STOCK QUOTE

        // ============================================

        if (plan.getTasks() != null

                && plan.getTasks().contains("LIVE_QUOTE")) {

            if (plan.getSymbol() == null

                    || plan.getSymbol().isBlank()) {

                liveQuoteResult =

                        "NO_LIVE_QUOTE_RESULT";

            } else {

                try {

                    var quote =

                            stockAnalysisTools.getLiveStockQuote(

                                    plan.getSymbol()

                            );

                    if (quote == null) {

                        liveQuoteResult =

                                "NO_LIVE_QUOTE_RESULT";

                    } else {

                        liveQuoteResult =

                                objectMapper.writeValueAsString(

                                        quote

                                );

                    }

                } catch (Exception e) {

                    System.err.println(

                            "LIVE QUOTE FAILED: "

                                    + e.getMessage()

                    );

                    liveQuoteResult =

                            "NO_LIVE_QUOTE_RESULT";

                }

            }

            System.out.println(

                    "LIVE QUOTE TOOL EXECUTED"

            );

            System.out.println(

                    "LIVE QUOTE RESULT: "

                            + liveQuoteResult

            );

        }

        // ============================================

        // 7. STOCK RECOMMENDATION

        // ============================================

        //

        // IMPORTANT:

        //

        // Investment allocation requests must also execute

        // the stock recommendation engine.

        //

        // This condition intentionally supports:

        //

        // STOCK_RECOMMENDATION

        // OR

        // INVESTMENT_ALLOCATION

        // OR

        // investmentAmount != null

        //

        // ============================================

        boolean recommendationRequired =

                plan.getTasks() != null

                        && (

                        plan.getTasks().contains(

                                "STOCK_RECOMMENDATION"

                        )

                        || plan.getTasks().contains(

                                "INVESTMENT_ALLOCATION"

                        )

                        || plan.getInvestmentAmount() != null

                );

        if (recommendationRequired) {

            try {

                // --------------------------------------------

                // Determine sector

                // --------------------------------------------

                String recommendationSector;

                if (plan.getSector() != null

                        && !plan.getSector().isBlank()) {

                    recommendationSector =

                            normalizeSector(plan.getSector());

                } else {

                    recommendationSector =

                            extractRecommendationSector(

                                    userMessage

                            );

                }

                // --------------------------------------------

                // Determine stock count

                // --------------------------------------------

                Integer requestedStockCount =

                        plan.getStockCount();

                if (requestedStockCount == null

                        || requestedStockCount <= 0) {

                    requestedStockCount = 5;

                }

                // Safety limit

                if (requestedStockCount > 50) {

                    requestedStockCount = 50;

                }

                System.out.println(

                        "RECOMMENDATION SECTOR: "

                                + recommendationSector

                );

                System.out.println(

                        "RECOMMENDATION STOCK COUNT: "

                                + requestedStockCount

                );

                // --------------------------------------------

                // Execute existing recommendation engine

                // --------------------------------------------

                Object results =

                        stockAnalysisTools.recommendStocksOnline(

                                recommendationSector,

                                requestedStockCount

                        );

                if (results == null) {

                    recommendationResult =

                            "NO_RECOMMENDATION_RESULT";

                } else {

                    recommendationResult =

                            objectMapper.writeValueAsString(

                                    results

                            );

                    // ----------------------------------------

                    // Investment allocation

                    // ----------------------------------------

                    if (plan.getInvestmentAmount() != null

                            && plan.getInvestmentAmount() > 0) {

                        recommendationResult =

                                addEqualInvestmentAllocation(

                                        recommendationResult,

                                        plan.getInvestmentAmount(),

                                        requestedStockCount

                                );

                    }

                }

            } catch (Exception e) {

                System.err.println(

                        "STOCK RECOMMENDATION FAILED: "

                                + e.getMessage()

                );

                e.printStackTrace();

                recommendationResult =

                        "NO_RECOMMENDATION_RESULT";

            }

            System.out.println(

                    "STOCK RECOMMENDATION TOOL EXECUTED"

            );

            System.out.println(

                    "RECOMMENDATION RESULT: "

                            + recommendationResult

            );

        }

        // ============================================

        // 8. FINAL COORDINATOR

        // ============================================

        System.out.println("=================================");

        System.out.println("COORDINATOR SYNTHESIS");

        System.out.println("=================================");

        return chatClient.prompt()

                // ====================================

                // COORDINATOR SYSTEM PROMPT

                // ====================================

                .system("""

                        You are the Coordinator Agent of MarketAI.

                        Your responsibility is to combine results

                        returned by specialized agents and produce

                        ONE final response for the user.

                        ============================================

                        AGENT CONTRACTS

                        ============================================

                        Research Agent:

                        Provides financial knowledge retrieved

                        from the MarketAI RAG knowledge base.

                        Analysis Agent:

                        Provides live stock and technical information

                        obtained through application tools.

                        Stock Comparison:

                        Provides live technical analysis results for

                        multiple requested stocks.

                        Live Quote:

                        Provides current market quote information.

                        Stock Recommendation:

                        Provides stocks returned by the application's

                        recommendation engine.

                        Investment Allocation:

                        Provides an allocation based on the stocks

                        returned by the recommendation engine.

                        ============================================

                        STRICT RULES

                        ============================================

                        1. Use ONLY information contained in the

                           agent results provided to you.

                        2. NEVER invent missing information.

                        3. NEVER calculate or estimate stock values.

                        4. Preserve numerical values exactly as

                           returned by the agents.

                        5. If Analysis Agent returns:

                           NO_ANALYSIS_RESULT

                           do not provide stock-specific technical

                           values from the Analysis Agent.

                        6. If Research Agent returns:

                           NO_RESEARCH_RESULT

                           do not claim that RAG knowledge was

                           retrieved.

                        7. Clearly distinguish live market analysis

                           from general financial knowledge.

                        8. Do not expose internal Java exceptions,

                           stack traces or implementation details.

                        9. Do not make guaranteed investment

                           predictions.

                        10. Answer the user's actual question directly.

                        11. Do not say that information was not

                            provided if a relevant agent result

                            actually contains the required information.

                        ============================================

                        STOCK COMPARISON RULES

                        ============================================

                        12. For STOCK_COMPARISON requests, use the

                            STOCK COMPARISON RESULT as the

                            authoritative source.

                        13. Compare requested stocks using ONLY

                            values present in the comparison result.

                        14. Preserve numerical values exactly.

                        15. Never invent missing values.

                        16. Present multiple stocks in a clear

                            side-by-side table when appropriate.

                        17. Do not require Research Agent Result or

                            Analysis Agent Result for a comparison.

                        18. If valid comparison data exists, answer

                            the comparison directly.

                        ============================================

                        LIVE QUOTE RULES

                        ============================================

                        19. For LIVE_QUOTE requests, use the

                            LIVE QUOTE RESULT as the authoritative

                            source.

                        20. Preserve live quote values exactly.

                        21. Never invent or estimate a stock price.

                        22. Do not require Research Agent Result or

                            Analysis Agent Result for a live quote.

                        23. If valid live quote data exists, answer

                            the user's question directly.

                        ============================================

                        STOCK RECOMMENDATION RULES

                        ============================================

                        24. For STOCK_RECOMMENDATION requests, use

                            the STOCK RECOMMENDATION RESULT as the

                            authoritative source.

                        25. Use only stocks and values returned by

                            the recommendation result.

                        26. Never invent stocks, prices, scores,

                            technical values or sectors.

                        27. Do not calculate replacement values.

                        28. If valid recommendation data exists,

                            present it clearly.

                        29. Do not claim recommendation data is

                            unavailable when valid data exists.

                        30. Do not present recommendations as

                            guaranteed investment outcomes.

                        ============================================

                        INVESTMENT ALLOCATION RULES

                        ============================================

                        31. For INVESTMENT_ALLOCATION requests, use

                            the STOCK RECOMMENDATION RESULT and its

                            investmentAllocation information.

                        32. Use ONLY stocks returned by the

                            recommendation engine.

                        33. Preserve allocation amounts exactly.

                        34. Preserve the requested investment amount

                            exactly.

                        35. Treat the investmentAllocation object as the

                            authoritative allocation result.

                        36. The stockCount in investmentAllocation is the

                            ACTUAL number of stocks receiving allocation.

                        37. requestedStockCount is only the user's requested

                            number and must not be presented as the actual

                            allocated stock count when risk filtering reduces it.

                        38. If investmentAllocation is present, clearly show:

                            - investment amount

                            - sector

                            - actual number of allocated stocks

                            - requested stock count when useful

                            - allocation method

                            - amount allocated per eligible stock

                            - selected allocated stocks

                            - score

                            - decision

                            - confidence

                            - allocated amount

                        39. Clearly identify stocks excluded by the risk filter

                            when excludedCandidates is present.

                        40. Do not invent additional stocks.

                        41. Do not replace stocks with other stocks.

                        42. Do not calculate a different allocation.

                        43. Explain that the allocation is generated

                            from the application's technical/performance

                            screening and is not a guaranteed investment

                            outcome.

                        ============================================

                        FINAL RESPONSE

                        ============================================

                        Return a clean, professional and concise

                        response.

                        Do not mention internal Java classes,

                        ObjectMapper, implementation details,

                        debugging logs or internal agent mechanics.

                        Answer the user's actual question directly.

                        """)

                // ====================================

                // COORDINATOR USER CONTEXT

                // ====================================

                .user("""

                        USER QUESTION:

                        %s

                        ============================================

                        AGENT PLAN

                        ============================================

                        INTENT:

                        %s

                        SYMBOL:

                        %s

                        SYMBOLS:

                        %s

                        TASKS:

                        %s

                        INVESTMENT AMOUNT:

                        %s

                        SECTOR:

                        %s

                        STOCK COUNT:

                        %s

                        ============================================

                        RESEARCH AGENT RESULT

                        ============================================

                        %s

                        ============================================

                        ANALYSIS AGENT RESULT

                        ============================================

                        %s

                        ============================================

                        STOCK COMPARISON RESULT

                        ============================================

                        %s

                        ============================================

                        LIVE QUOTE RESULT

                        ============================================

                        %s

                        ============================================

                        STOCK RECOMMENDATION RESULT

                        ============================================

                        %s

                        ============================================

                        IMPORTANT

                        ============================================

                        Use the relevant result above to answer

                        the user's question.

                        For STOCK_COMPARISON requests, use

                        STOCK COMPARISON RESULT.

                        For LIVE_QUOTE requests, use

                        LIVE QUOTE RESULT.

                        For STOCK_RECOMMENDATION requests, use

                        STOCK RECOMMENDATION RESULT.

                        For INVESTMENT_ALLOCATION requests, use

                        the investment allocation contained inside

                        STOCK RECOMMENDATION RESULT.

                        Produce the final answer using ONLY the

                        information provided above.

                        Do not claim information is unavailable

                        when the relevant result contains valid data.

                        """.formatted(

                        userMessage,

                        plan.getIntent(),

                        plan.getSymbol(),

                        plan.getSymbols(),

                        plan.getTasks(),

                        plan.getInvestmentAmount(),

                        plan.getSector(),

                        plan.getStockCount(),

                        researchResult,

                        analysisResult,

                        comparisonResult,

                        liveQuoteResult,

                        recommendationResult

                ))

                .call()

                .content();

    }

    // ============================================================

    // INVESTMENT ALLOCATION

    // ============================================================

    private String addEqualInvestmentAllocation(
            String recommendationJson,
            double investmentAmount,
            int requestedStockCount) {

        try {
            ObjectNode root =
                    (ObjectNode) objectMapper.readTree(recommendationJson);

            // StockRecommendationService serializes its list as "candidates".
            // Accept "topCandidates" as well for backward compatibility.
            JsonNode candidates = root.get("candidates");
            if (candidates == null || !candidates.isArray()) {
                candidates = root.get("topCandidates");
            }

            JsonNode candidateDecisions = root.get("candidateDecisions");

            if (candidates == null
                    || !candidates.isArray()
                    || candidates.isEmpty()) {
                return recommendationJson;
            }

            ArrayNode eligibleCandidates =
                    objectMapper.createArrayNode();

            ArrayNode excludedCandidates =
                    objectMapper.createArrayNode();

            for (JsonNode candidate : candidates) {

                if (candidate == null || !candidate.isObject()) {
                    continue;
                }

                String symbol = getCandidateSymbol(candidate);
                String decision = getCandidateDecision(
                        symbol,
                        candidateDecisions);
                String confidence = getCandidateConfidence(
                        symbol,
                        candidateDecisions);

                ObjectNode candidateStatus =
                        objectMapper.createObjectNode();
                candidateStatus.put("symbol", symbol == null ? "" : symbol);
                candidateStatus.put("decision", decision);
                candidateStatus.put("confidence", confidence);

                if ("DO NOT BUY".equalsIgnoreCase(decision)) {
                    excludedCandidates.add(candidateStatus);
                } else {
                    eligibleCandidates.add(candidate);
                }
            }

            int requestedCount = Math.max(requestedStockCount, 1);

            // Never allocate to a stock whose risk guard says DO NOT BUY.
            int actualStockCount = Math.min(
                    requestedCount,
                    eligibleCandidates.size()
            );

            double allocationPerStock =
                    actualStockCount > 0
                            ? investmentAmount / actualStockCount
                            : 0.0;

            double allocatedTotal =
                    actualStockCount * allocationPerStock;

            double unallocatedAmount =
                    investmentAmount - allocatedTotal;

            root.put("investmentAmount", investmentAmount);
            root.put("requestedStockCount", requestedStockCount);
            root.put("stockCount", actualStockCount);
            root.put(
                    "allocationMethod",
                    "EQUAL_AMONG_RISK_ELIGIBLE_STOCKS"
            );
            root.put(
                    "allocationPerStock",
                    roundAmount(allocationPerStock)
            );
            root.put(
                    "allocatedTotal",
                    roundAmount(allocatedTotal)
            );
            root.put(
                    "unallocatedAmount",
                    roundAmount(unallocatedAmount)
            );
            root.put(
                    "selectionMethod",
                    "TOP_PERFORMERS_BY_TECHNICAL_SCORE_WITH_RISK_FILTER"
            );

            root.set("excludedCandidates", excludedCandidates);

            ArrayNode allocationArray =
                    objectMapper.createArrayNode();

            for (int i = 0; i < actualStockCount; i++) {

                JsonNode candidate = eligibleCandidates.get(i);

                String symbol = getCandidateSymbol(candidate);

                double score = candidate.has("score")
                        ? candidate.get("score").asDouble()
                        : 0.0;

                String decision = getCandidateDecision(
                        symbol,
                        candidateDecisions);

                String confidence = getCandidateConfidence(
                        symbol,
                        candidateDecisions);

                ObjectNode allocation =
                        objectMapper.createObjectNode();

                allocation.put("rank", i + 1);
                allocation.put("symbol", symbol == null ? "" : symbol);
                allocation.put("score", score);
                allocation.put("decision", decision);
                allocation.put("confidence", confidence);
                allocation.put(
                        "allocatedAmount",
                        roundAmount(allocationPerStock)
                );

                allocationArray.add(allocation);
            }

            root.set("investmentAllocation", allocationArray);

            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(root);

        } catch (Exception e) {

            System.err.println(
                    "Investment allocation processing failed: "
                            + e.getMessage()
            );

            return recommendationJson;
        }
    }

    private String getCandidateSymbol(JsonNode candidate) {

        if (candidate == null) {
            return null;
        }

        if (candidate.has("symbol")) {
            return candidate.get("symbol").asText();
        }

        if (candidate.has("stockSymbol")) {
            return candidate.get("stockSymbol").asText();
        }

        return null;
    }

    private String getCandidateDecision(
            String symbol,
            JsonNode candidateDecisions) {

        if (symbol == null
                || candidateDecisions == null
                || !candidateDecisions.isArray()) {
            return "Not specified";
        }

        for (JsonNode candidateDecision : candidateDecisions) {

            if (candidateDecision == null
                    || !candidateDecision.has("symbol")) {
                continue;
            }

            String decisionSymbol =
                    candidateDecision.get("symbol").asText();

            if (symbol.equalsIgnoreCase(decisionSymbol)
                    && candidateDecision.has("decision")) {
                return candidateDecision
                        .get("decision")
                        .asText();
            }
        }

        return "Not specified";
    }

    private String getCandidateConfidence(
            String symbol,
            JsonNode candidateDecisions) {

        if (symbol == null
                || candidateDecisions == null
                || !candidateDecisions.isArray()) {
            return "Not specified";
        }

        for (JsonNode candidateDecision : candidateDecisions) {

            if (candidateDecision == null
                    || !candidateDecision.has("symbol")) {
                continue;
            }

            String decisionSymbol =
                    candidateDecision.get("symbol").asText();

            if (symbol.equalsIgnoreCase(decisionSymbol)
                    && candidateDecision.has("confidence")) {
                return candidateDecision
                        .get("confidence")
                        .asText();
            }
        }

        return "Not specified";
    }

    private double roundAmount(double amount) {

        return Math.round(

                amount * 100.0

        ) / 100.0;

    }

    // ============================================================

    // SECTOR NORMALIZATION

    // ============================================================

    private String normalizeSector(String sector) {

        if (sector == null || sector.isBlank()) {

            return "ALL";

        }

        String normalized =

                sector.trim()

                        .toUpperCase();

        if (normalized.equals("INFORMATION TECHNOLOGY")

                || normalized.equals("TECH")

                || normalized.equals("TECHNOLOGY")) {

            return "IT";

        }

        if (normalized.equals("BANK")

                || normalized.equals("BANKS")) {

            return "BANKING";

        }

        if (normalized.equals("PHARMACEUTICAL")

                || normalized.equals("PHARMACEUTICALS")) {

            return "PHARMA";

        }

        if (normalized.equals("AUTOMOBILE")

                || normalized.equals("AUTOMOBILES")) {

            return "AUTO";

        }

        if (normalized.equals("METALS")) {

            return "METAL";

        }

        if (normalized.equals("REAL ESTATE")) {

            return "REALTY";

        }

        if (normalized.equals("FINANCIAL")) {

            return "FINANCE";

        }

        return normalized;

    }

    // ============================================================

    // EXTRACT SECTOR FROM USER MESSAGE

    // ============================================================

    private String extractRecommendationSector(

            String userMessage) {

        if (userMessage == null

                || userMessage.isBlank()) {

            return "ALL";

        }

        String query =

                userMessage.toUpperCase();

        if (query.contains("INFORMATION TECHNOLOGY")

                || query.contains(" IT ")

                || query.endsWith(" IT")

                || query.startsWith("IT ")

                || query.contains("TECHNOLOGY")

                || query.contains("TECH STOCK")

                || query.contains("TECH STOCKS")) {

            return "IT";

        }

        if (query.contains("BANKING")

                || query.contains("BANK STOCK")

                || query.contains("BANK STOCKS")) {

            return "BANKING";

        }

        if (query.contains("PHARMA")

                || query.contains("PHARMACEUTICAL")) {

            return "PHARMA";

        }

        if (query.contains("AUTO")

                || query.contains("AUTOMOBILE")) {

            return "AUTO";

        }

        if (query.contains("FMCG")) {

            return "FMCG";

        }

        if (query.contains("METAL")

                || query.contains("METALS")) {

            return "METAL";

        }

        if (query.contains("REALTY")

                || query.contains("REAL ESTATE")) {

            return "REALTY";

        }

        if (query.contains("MEDIA")) {

            return "MEDIA";

        }

        if (query.contains("ENERGY")) {

            return "ENERGY";

        }

        if (query.contains("PSU BANK")

                || query.contains("PSU BANKS")) {

            return "PSU BANK";

        }

        if (query.contains("PRIVATE BANK")

                || query.contains("PRIVATE BANKS")) {

            return "PRIVATE BANK";

        }

        if (query.contains("FINANCE")

                || query.contains("FINANCIAL")) {

            return "FINANCE";

        }

        return "ALL";

    }

}