package com.ram.ai.stockagent.model;

public class StockAnalysis {

    // =========================================================
    // BASIC STOCK INFORMATION
    // =========================================================

    private String symbol;
    private String companyName;
    private String sector;

    // =========================================================
    // MARKET DATA
    // =========================================================

    private Double price;
    private Double currentPrice;
    private Double previousClose;
    private Double changePercent;

    private Double open;
    private Double high;
    private Double low;

    // =========================================================
    // VOLUME
    // =========================================================

    private Long currentVolume;
    private Double averageVolume20;
    private Double volumeRatio;

    // =========================================================
    // MOVING AVERAGES
    // =========================================================

    private Double sma20;
    private Double sma50;
    private Double ema20;

    // =========================================================
    // RSI
    // =========================================================

    private Double rsi14;

    // =========================================================
    // MACD
    // =========================================================

    private Double macd;
    private Double macdSignal;
    private Double macdHistogram;

    // =========================================================
    // STATUS
    // =========================================================

    private String momentum;
    private String volumeStrength;
    private String trend;
    private String rsiStatus;
    private String macdStatus;

    // =========================================================
    // SCORE
    // =========================================================

    private Double score;

    private Double momentumPoints;
    private Double volumePoints;
    private Double trendPoints;
    private Double rsiPoints;
    private Double macdPoints;
    private Double priceStrengthPoints;

    // =========================================================
    // DEFAULT CONSTRUCTOR
    // =========================================================

    public StockAnalysis() {
    }

    // =========================================================
    // COMPLETE CONSTRUCTOR
    // =========================================================

    public StockAnalysis(
            String symbol,
            String companyName,
            String sector,

            Double price,
            Double currentPrice,
            Double previousClose,
            Double changePercent,

            Double open,
            Double high,
            Double low,

            Long currentVolume,
            Double averageVolume20,
            Double volumeRatio,

            Double sma20,
            Double sma50,
            Double ema20,

            Double rsi14,

            Double macd,
            Double macdSignal,
            Double macdHistogram,

            String momentum,
            String volumeStrength,
            String trend,
            String rsiStatus,
            String macdStatus,

            Double score,

            Double momentumPoints,
            Double volumePoints,
            Double trendPoints,
            Double rsiPoints,
            Double macdPoints,
            Double priceStrengthPoints) {

        this.symbol = symbol;
        this.companyName = companyName;
        this.sector = sector;

        this.price = price;
        this.currentPrice = currentPrice;
        this.previousClose = previousClose;
        this.changePercent = changePercent;

        this.open = open;
        this.high = high;
        this.low = low;

        this.currentVolume = currentVolume;
        this.averageVolume20 = averageVolume20;
        this.volumeRatio = volumeRatio;

        this.sma20 = sma20;
        this.sma50 = sma50;
        this.ema20 = ema20;

        this.rsi14 = rsi14;

        this.macd = macd;
        this.macdSignal = macdSignal;
        this.macdHistogram = macdHistogram;

        this.momentum = momentum;
        this.volumeStrength = volumeStrength;
        this.trend = trend;
        this.rsiStatus = rsiStatus;
        this.macdStatus = macdStatus;

        this.score = score;

        this.momentumPoints = momentumPoints;
        this.volumePoints = volumePoints;
        this.trendPoints = trendPoints;
        this.rsiPoints = rsiPoints;
        this.macdPoints = macdPoints;
        this.priceStrengthPoints = priceStrengthPoints;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getSector() {
        return sector;
    }

    public Double getPrice() {
        return price;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public Double getPreviousClose() {
        return previousClose;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public Double getOpen() {
        return open;
    }

    public Double getHigh() {
        return high;
    }

    public Double getLow() {
        return low;
    }

    public Long getCurrentVolume() {
        return currentVolume;
    }

    public Double getAverageVolume20() {
        return averageVolume20;
    }

    public Double getVolumeRatio() {
        return volumeRatio;
    }

    public Double getSma20() {
        return sma20;
    }

    public Double getSma50() {
        return sma50;
    }

    public Double getEma20() {
        return ema20;
    }

    public Double getRsi14() {
        return rsi14;
    }

    public Double getMacd() {
        return macd;
    }

    public Double getMacdSignal() {
        return macdSignal;
    }

    public Double getMacdHistogram() {
        return macdHistogram;
    }

    public String getMomentum() {
        return momentum;
    }

    public String getVolumeStrength() {
        return volumeStrength;
    }

    public String getTrend() {
        return trend;
    }

    public String getRsiStatus() {
        return rsiStatus;
    }

    public String getMacdStatus() {
        return macdStatus;
    }

    public Double getScore() {
        return score;
    }

    public Double getMomentumPoints() {
        return momentumPoints;
    }

    public Double getVolumePoints() {
        return volumePoints;
    }

    public Double getTrendPoints() {
        return trendPoints;
    }

    public Double getRsiPoints() {
        return rsiPoints;
    }

    public Double getMacdPoints() {
        return macdPoints;
    }

    public Double getPriceStrengthPoints() {
        return priceStrengthPoints;
    }

    // =========================================================
    // SETTERS
    // =========================================================

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setPreviousClose(Double previousClose) {
        this.previousClose = previousClose;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public void setHigh(Double high) {
        this.high = high;
    }

    public void setLow(Double low) {
        this.low = low;
    }

    public void setCurrentVolume(Long currentVolume) {
        this.currentVolume = currentVolume;
    }

    public void setAverageVolume20(Double averageVolume20) {
        this.averageVolume20 = averageVolume20;
    }

    public void setVolumeRatio(Double volumeRatio) {
        this.volumeRatio = volumeRatio;
    }

    public void setSma20(Double sma20) {
        this.sma20 = sma20;
    }

    public void setSma50(Double sma50) {
        this.sma50 = sma50;
    }

    public void setEma20(Double ema20) {
        this.ema20 = ema20;
    }

    public void setRsi14(Double rsi14) {
        this.rsi14 = rsi14;
    }

    public void setMacd(Double macd) {
        this.macd = macd;
    }

    public void setMacdSignal(Double macdSignal) {
        this.macdSignal = macdSignal;
    }

    public void setMacdHistogram(Double macdHistogram) {
        this.macdHistogram = macdHistogram;
    }

    public void setMomentum(String momentum) {
        this.momentum = momentum;
    }

    public void setVolumeStrength(String volumeStrength) {
        this.volumeStrength = volumeStrength;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public void setRsiStatus(String rsiStatus) {
        this.rsiStatus = rsiStatus;
    }

    public void setMacdStatus(String macdStatus) {
        this.macdStatus = macdStatus;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public void setMomentumPoints(Double momentumPoints) {
        this.momentumPoints = momentumPoints;
    }

    public void setVolumePoints(Double volumePoints) {
        this.volumePoints = volumePoints;
    }

    public void setTrendPoints(Double trendPoints) {
        this.trendPoints = trendPoints;
    }

    public void setRsiPoints(Double rsiPoints) {
        this.rsiPoints = rsiPoints;
    }

    public void setMacdPoints(Double macdPoints) {
        this.macdPoints = macdPoints;
    }

    public void setPriceStrengthPoints(Double priceStrengthPoints) {
        this.priceStrengthPoints = priceStrengthPoints;
    }
}