package com.ram.ai.stockagent.analysis;

public class TechnicalIndicators {

    private final String symbol;

    private final double currentPrice;

    private final double sma20;

    private final double sma50;

    private final double ema20;

    private final double rsi14;

    private final double macd;

    private final double macdSignal;

    private final double macdHistogram;

    private final long currentVolume;

    private final double averageVolume20;

    private final double volumeRatio;

    private final String rsiStatus;

    private final String volumeStatus;

    private final String trend;

    public TechnicalIndicators(
            String symbol,
            double currentPrice,
            double sma20,
            double sma50,
            double ema20,
            double rsi14,
            double macd,
            double macdSignal,
            double macdHistogram,
            long currentVolume,
            double averageVolume20,
            double volumeRatio,
            String rsiStatus,
            String volumeStatus,
            String trend) {

        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.sma20 = sma20;
        this.sma50 = sma50;
        this.ema20 = ema20;
        this.rsi14 = rsi14;
        this.macd = macd;
        this.macdSignal = macdSignal;
        this.macdHistogram = macdHistogram;
        this.currentVolume = currentVolume;
        this.averageVolume20 = averageVolume20;
        this.volumeRatio = volumeRatio;
        this.rsiStatus = rsiStatus;
        this.volumeStatus = volumeStatus;
        this.trend = trend;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getSma20() {
        return sma20;
    }

    public double getSma50() {
        return sma50;
    }

    public double getEma20() {
        return ema20;
    }

    public double getRsi14() {
        return rsi14;
    }

    public double getMacd() {
        return macd;
    }

    public double getMacdSignal() {
        return macdSignal;
    }

    public double getMacdHistogram() {
        return macdHistogram;
    }

    public long getCurrentVolume() {
        return currentVolume;
    }

    public double getAverageVolume20() {
        return averageVolume20;
    }

    public double getVolumeRatio() {
        return volumeRatio;
    }

    public String getRsiStatus() {
        return rsiStatus;
    }

    public String getVolumeStatus() {
        return volumeStatus;
    }

    public String getTrend() {
        return trend;
    }
}