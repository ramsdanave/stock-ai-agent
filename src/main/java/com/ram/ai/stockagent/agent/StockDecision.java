package com.ram.ai.stockagent.agent;

public class StockDecision {

    private String symbol;

    private String decision;

    private String confidence;

    private double score;

    private String reason;

    public StockDecision() {
    }

    public StockDecision(
            String symbol,
            String decision,
            String confidence,
            double score,
            String reason) {

        this.symbol = symbol;
        this.decision = decision;
        this.confidence = confidence;
        this.score = score;
        this.reason = reason;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {

        return "StockDecision{" +
                "symbol='" + symbol + '\'' +
                ", decision='" + decision + '\'' +
                ", confidence='" + confidence + '\'' +
                ", score=" + score +
                ", reason='" + reason + '\'' +
                '}';
    }
}