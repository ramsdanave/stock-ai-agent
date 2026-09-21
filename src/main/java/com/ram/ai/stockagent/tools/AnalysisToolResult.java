package com.ram.ai.stockagent.tools;

public class AnalysisToolResult {

    private String status;
    private String symbol;
    private String message;

    public AnalysisToolResult() {
    }

    public AnalysisToolResult(
            String status,
            String symbol,
            String message) {

        this.status = status;
        this.symbol = symbol;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getMessage() {
        return message;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}