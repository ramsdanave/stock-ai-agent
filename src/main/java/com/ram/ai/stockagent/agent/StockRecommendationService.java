package com.ram.ai.stockagent.agent;

import com.ram.ai.stockagent.analysis.StockAnalysisService;
import com.ram.ai.stockagent.market.OnlineStockDiscoveryService;
import com.ram.ai.stockagent.model.StockAnalysis;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StockRecommendationService {

    private final OnlineStockDiscoveryService onlineStockDiscoveryService;

    private final StockAnalysisService stockAnalysisService;

    private final StockRiskService stockRiskService;

    public StockRecommendationService(
            OnlineStockDiscoveryService onlineStockDiscoveryService,
            StockAnalysisService stockAnalysisService,
            StockRiskService stockRiskService) {

        this.onlineStockDiscoveryService =
                onlineStockDiscoveryService;

        this.stockAnalysisService =
                stockAnalysisService;

        this.stockRiskService =
                stockRiskService;
    }

    // =========================================================
    // MAIN RECOMMENDATION METHOD
    // =========================================================

    public StockRecommendation recommend(
            String sector,
            int limit) {

        if (sector == null
                || sector.trim().isEmpty()) {

            sector = "ALL";
        }

        sector =
                sector.trim().toUpperCase();

        if (limit <= 0) {
            limit = 5;
        }

        // =====================================================
        // 1. DISCOVER STOCKS ONLINE
        // =====================================================

        List<String> symbols =
                onlineStockDiscoveryService
                        .discoverStocks(sector);

        if (symbols == null
                || symbols.isEmpty()) {

            return StockRecommendation.noData(
                    sector
            );
        }

        // =====================================================
        // 2. ANALYZE EACH ONLINE STOCK
        // =====================================================

        List<StockAnalysis> analyses =
                new ArrayList<>();

        for (String symbol : symbols) {

            if (symbol == null
                    || symbol.trim().isEmpty()) {

                continue;
            }

            try {

                StockAnalysis analysis =
                        stockAnalysisService.analyzeStock(
                                symbol.trim().toUpperCase()
                        );

                if (analysis != null) {

                    analyses.add(analysis);
                }

            } catch (Exception e) {

                System.err.println(
                        "Analysis failed for "
                                + symbol
                                + ": "
                                + e.getMessage()
                );
            }
        }

        // =====================================================
        // 3. NO VALID ANALYSIS
        // =====================================================

        if (analyses.isEmpty()) {

            return StockRecommendation.noData(
                    sector
            );
        }

        // =====================================================
        // 4. RANK BY TECHNICAL SCORE
        // =====================================================

        analyses.sort(
                Comparator.comparing(
                        StockAnalysis::getScore,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        // =====================================================
        // 5. KEEP TOP CANDIDATES
        // =====================================================

        int actualLimit =
                Math.min(
                        limit,
                        analyses.size()
                );

        List<StockAnalysis> topCandidates =
                new ArrayList<>(
                        analyses.subList(
                                0,
                                actualLimit
                        )
                );

        // =====================================================
        // 6. SELECT BEST TECHNICAL CANDIDATE
        // =====================================================

        StockAnalysis best =
                topCandidates.get(0);

        // =====================================================
        // 7. CREATE DECISION WITH RISK CONTROL
        // =====================================================

        StockDecision decision =
                createDecision(best);
        
     // =====================================================
     // CREATE DECISIONS FOR ALL TOP CANDIDATES
     // =====================================================

     List<StockDecision> candidateDecisions =
             new ArrayList<>();

     for (StockAnalysis candidate : topCandidates) {

         if (candidate == null) {
             continue;
         }

         candidateDecisions.add(
                 createDecision(candidate)
         );
     }

        // =====================================================
        // 8. RETURN COMPLETE RECOMMENDATION
        // =====================================================

        return new StockRecommendation(
                sector,
                decision,
                topCandidates,
                candidateDecisions,
                analyses.size(),
                buildSummary(
                        sector,
                        best,
                        decision
                )
        );
    }

    // =========================================================
    // DECISION ENGINE + RISK CONTROL
    // =========================================================

    private StockDecision createDecision(
            StockAnalysis analysis) {

        double score =
                analysis.getScore();

        String decision;

        String confidence;

        String reason;

        // -----------------------------------------------------
        // FIRST: EXISTING TECHNICAL SCORE DECISION
        // -----------------------------------------------------

        if (score >= 75) {

            decision =
                    "STRONG CANDIDATE";

            confidence =
                    "HIGH";

            reason =
                    "The stock has a strong quantitative "
                            + "technical score and currently ranks "
                            + "among the strongest candidates.";

        } else if (score >= 60) {

            decision =
                    "CONSIDER";

            confidence =
                    "MEDIUM";

            reason =
                    "The stock shows relatively strong "
                            + "technical characteristics, but "
                            + "additional confirmation is preferred.";

        } else if (score >= 45) {

            decision =
                    "WATCH";

            confidence =
                    "LOW";

            reason =
                    "The stock currently has a mixed or "
                            + "neutral technical setup. A stronger "
                            + "confirmation signal is preferred.";

        } else {

            decision =
                    "AVOID / WAIT";

            confidence =
                    "LOW";

            reason =
                    "The current quantitative score does "
                            + "not provide sufficient technical "
                            + "strength for consideration.";
        }

        // -----------------------------------------------------
        // SECOND: RISK ASSESSMENT
        // -----------------------------------------------------

        StockRiskService.RiskAssessment risk =
                stockRiskService.assess(
                        analysis
                );

        if (risk != null) {

            String riskGuard =
                    risk.getRecommendationGuard();

            String riskLevel =
                    risk.getRiskLevel();

            String riskReason =
                    risk.getRiskReason();

            // -------------------------------------------------
            // RISK OVERRIDES
            // -------------------------------------------------

            if ("DO NOT BUY".equalsIgnoreCase(
                    riskGuard)) {

                decision =
                        "DO NOT BUY";

                confidence =
                        "LOW";

                reason =
                        riskReason;

            } else if ("WAIT FOR CONFIRMATION"
                    .equalsIgnoreCase(
                            riskGuard)) {

                decision =
                        "WAIT FOR CONFIRMATION";

                confidence =
                        "LOW";

                reason =
                        riskReason;

            } else if ("WATCH".equalsIgnoreCase(
                    riskGuard)) {

                if (!"AVOID / WAIT".equalsIgnoreCase(
                        decision)) {

                    decision =
                            "WATCH";
                }

                confidence =
                        "LOW";

                reason =
                        riskReason;
            }

            // -------------------------------------------------
            // ADD RISK INFORMATION
            // -------------------------------------------------

            reason =
                    reason
                            + " Risk level: "
                            + riskLevel
                            + ".";
        }

        return new StockDecision(
                analysis.getSymbol(),
                decision,
                confidence,
                score,
                reason
        );
    }

    // =========================================================
    // SUMMARY
    // =========================================================

    private String buildSummary(
            String sector,
            StockAnalysis best,
            StockDecision decision) {

        return "Online market screening identified "
                + best.getSymbol()
                + " as the highest-ranked candidate "
                + "in the "
                + sector
                + " sector with a technical score of "
                + best.getScore()
                + ". Current decision: "
                + decision.getDecision()
                + " with "
                + decision.getConfidence()
                + " confidence.";
    }

    // =========================================================
    // RESULT OBJECT
    // =========================================================

    public static class StockRecommendation {

        private String sector;

        private StockDecision decision;

        private List<StockAnalysis> candidates;
        
        private List<StockDecision> candidateDecisions;

        private int totalCandidatesAnalyzed;

        private String summary;

        public StockRecommendation() {
        }

        public StockRecommendation(
                String sector,
                StockDecision decision,
                List<StockAnalysis> candidates,
                List<StockDecision> candidateDecisions,
                int totalCandidatesAnalyzed,
                String summary) {

            this.sector = sector;

            this.decision = decision;

            this.candidates = candidates;
            
            this.candidateDecisions = candidateDecisions;

            this.totalCandidatesAnalyzed =
                    totalCandidatesAnalyzed;

            this.summary = summary;
        }

        public static StockRecommendation noData(
                String sector) {

            StockDecision decision =
                    new StockDecision(
                            "",
                            "NO DATA",
                            "NONE",
                            0.0,
                            "No usable online stock analysis "
                                    + "was available for the "
                                    + sector
                                    + " sector."
                    );

            return new StockRecommendation(
                    sector,
                    decision,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    0,
                    "No online candidates were available "
                            + "for analysis."
            );
        }

        public String getSector() {
            return sector;
        }

        public void setSector(
                String sector) {

            this.sector = sector;
        }

        public StockDecision getDecision() {
            return decision;
        }

        public void setDecision(
                StockDecision decision) {

            this.decision = decision;
        }

        public List<StockAnalysis> getCandidates() {
            return candidates;
        }

        public void setCandidates(
                List<StockAnalysis> candidates) {

            this.candidates = candidates;
        }
        
        public List<StockDecision> getCandidateDecisions() {
            return candidateDecisions;
        }

        public void setCandidateDecisions(
                List<StockDecision> candidateDecisions) {

            this.candidateDecisions = candidateDecisions;
        }

        public int getTotalCandidatesAnalyzed() {
            return totalCandidatesAnalyzed;
        }

        public void setTotalCandidatesAnalyzed(
                int totalCandidatesAnalyzed) {

            this.totalCandidatesAnalyzed =
                    totalCandidatesAnalyzed;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(
                String summary) {

            this.summary = summary;
        }

        @Override
        public String toString() {

            return "StockRecommendation{"
                    + "sector='"
                    + sector
                    + '\''
                    + ", decision="
                    + decision
                    + ", candidates="
                    + candidates
                    + ", totalCandidatesAnalyzed="
                    + totalCandidatesAnalyzed
                    + ", summary='"
                    + summary
                    + '\''
                    + '}';
        }
    }
}