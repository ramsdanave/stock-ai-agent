package com.ram.ai.stockagent.agent;

import com.ram.ai.stockagent.model.StockAnalysis;
import org.springframework.stereotype.Service;

@Service
public class StockDecisionService {

    public StockDecision decide(StockAnalysis analysis) {

        if (analysis == null) {
            return new StockDecision(
                    "",
                    "NO DATA",
                    "NONE",
                    0.0,
                    "Stock analysis data is unavailable."
            );
        }

        double score = analysis.getScore();

        String symbol = analysis.getSymbol();

        String decision;
        String confidence;
        String reason;

        if (score >= 75) {

            decision = "BUY CANDIDATE";
            confidence = "HIGH";

            reason =
                    "The technical analysis shows strong overall strength. "
                    + "Multiple quantitative signals are supporting the stock.";

        } else if (score >= 60) {

            decision = "CONSIDER";
            confidence = "MEDIUM";

            reason =
                    "The stock has a relatively strong technical setup, "
                    + "but additional confirmation is preferred before entering.";

        } else if (score >= 45) {

            decision = "WATCH";
            confidence = "LOW";

            reason =
                    "The technical setup is mixed or neutral. "
                    + "Waiting for stronger confirmation is preferable.";

        } else {

            decision = "AVOID / WAIT";
            confidence = "LOW";

            reason =
                    "The current technical score does not indicate "
                    + "sufficient strength for a positive recommendation.";
        }

        return new StockDecision(
                symbol,
                decision,
                confidence,
                score,
                reason
        );
    }
}