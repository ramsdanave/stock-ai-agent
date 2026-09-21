package com.ram.ai.stockagent.agent;

public class StockMarketDecision {

    private String symbol;
    private String tradingSymbol;

    private double ltp;
    private double open;
    private double high;
    private double low;
    private double previousClose;

    private double existingScore;
    private double trendScore;
    private double pricePositionScore;
    private double marketScore;

    private boolean historicalDataAvailable;

    public StockMarketDecision() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTradingSymbol() {
        return tradingSymbol;
    }

    public void setTradingSymbol(String tradingSymbol) {
        this.tradingSymbol = tradingSymbol;
    }

    public double getLtp() {
        return ltp;
    }

    public void setLtp(double ltp) {
        this.ltp = ltp;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(double previousClose) {
        this.previousClose = previousClose;
    }

    public double getExistingScore() {
        return existingScore;
    }

    public void setExistingScore(double existingScore) {
        this.existingScore = existingScore;
    }

    public double getTrendScore() {
        return trendScore;
    }

    public void setTrendScore(double trendScore) {
        this.trendScore = trendScore;
    }

    public double getPricePositionScore() {
        return pricePositionScore;
    }

    public void setPricePositionScore(double pricePositionScore) {
        this.pricePositionScore = pricePositionScore;
    }

    public double getMarketScore() {
        return marketScore;
    }

    public void setMarketScore(double marketScore) {
        this.marketScore = marketScore;
    }

    public boolean isHistoricalDataAvailable() {
        return historicalDataAvailable;
    }

    public void setHistoricalDataAvailable(
            boolean historicalDataAvailable) {

        this.historicalDataAvailable =
                historicalDataAvailable;
    }
}