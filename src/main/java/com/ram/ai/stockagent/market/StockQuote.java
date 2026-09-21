package com.ram.ai.stockagent.market;

public class StockQuote {

    private String symbol;
    private String exchange;
    private String tradingSymbol;

    private double ltp;
    private double open;
    private double high;
    private double low;
    private double close;

    private boolean available;
    private String errorMessage;


    public StockQuote() {
    }


    public StockQuote(
            String symbol,
            String exchange,
            String tradingSymbol,
            double ltp,
            double open,
            double high,
            double low,
            double close) {

        this.symbol = symbol;
        this.exchange = exchange;
        this.tradingSymbol = tradingSymbol;

        this.ltp = ltp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;

        this.available = true;
        this.errorMessage = null;
    }


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }


    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
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


    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }


    // =========================================================
    // AVAILABILITY
    // =========================================================

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }


    // =========================================================
    // ERROR MESSAGE
    // =========================================================

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}