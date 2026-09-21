package com.ram.ai.stockagent.analysis;

import com.ram.ai.stockagent.market.MarketDataProvider;
import com.ram.ai.stockagent.market.StockQuote;
import com.ram.ai.stockagent.model.Stock;
import com.ram.ai.stockagent.model.StockAnalysis;
import com.ram.ai.stockagent.repository.StockRepository;

import org.springframework.stereotype.Service;

@Service
public class StockAnalysisService {

    private final StockRepository stockRepository;
    private final MarketDataProvider marketDataProvider;
    private final TechnicalAnalysisService technicalAnalysisService;

    public StockAnalysisService(
            StockRepository stockRepository,
            MarketDataProvider marketDataProvider,
            TechnicalAnalysisService technicalAnalysisService) {

        this.stockRepository = stockRepository;
        this.marketDataProvider = marketDataProvider;
        this.technicalAnalysisService = technicalAnalysisService;
    }

    // =========================================================
    // COMPATIBILITY METHOD
    // Used by StockScreenerService
    // =========================================================

    public StockAnalysis analyze(Stock stock) {

        if (stock == null || stock.getSymbol() == null
                || stock.getSymbol().isBlank()) {

            return null;
        }

        return analyzeStock(stock.getSymbol());
    }

    // =========================================================
    // MAIN STOCK ANALYSIS
    // =========================================================

    public StockAnalysis analyzeStock(String symbol) {

        if (symbol == null || symbol.isBlank()) {
            return null;
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        

        // =====================================================
        // 2. GET REAL-TIME MARKET DATA
        // =====================================================

        StockQuote quote =
                marketDataProvider.getQuote(
                        normalizedSymbol
                );

        if (quote == null) {

            throw new RuntimeException(
                    "Unable to retrieve live market data for "
                            + normalizedSymbol
            );
        }

        // =====================================================
        // 3. GET TECHNICAL INDICATORS
        // =====================================================

        TechnicalIndicators technical =
                technicalAnalysisService.analyze(
                        normalizedSymbol
                );

        if (technical == null) {

            throw new RuntimeException(
                    "Unable to calculate technical indicators for "
                            + normalizedSymbol
            );
        }

        // =====================================================
        // 4. LIVE PRICE
        // =====================================================

        double livePrice =
                quote.getLtp();

        // =====================================================
        // 5. PREVIOUS CLOSE
        // =====================================================

        double previousClose =
                quote.getClose();

        // =====================================================
        // 6. CHANGE PERCENT
        // =====================================================

        double changePercent = 0.0;

        if (previousClose != 0) {

            changePercent =
                    ((livePrice - previousClose)
                            / previousClose)
                            * 100.0;
        }

        // =====================================================
        // 7. MOMENTUM
        // =====================================================

        String momentum =
                determineMomentum(
                        changePercent,
                        technical.getRsi14(),
                        technical.getMacdHistogram()
                );

        // =====================================================
        // 8. MACD STATUS
        // =====================================================

        String macdStatus =
                determineMacdStatus(
                        technical.getMacd(),
                        technical.getMacdSignal(),
                        technical.getMacdHistogram()
                );

        // =====================================================
        // 9. CALCULATE SCORE
        // =====================================================

        ScoreResult score =
                calculateScore(
                        livePrice,
                        technical,
                        changePercent
                );

        // =====================================================
        // 10. RETURN COMPLETE ANALYSIS
        // =====================================================

        return new StockAnalysis(

                // -------------------------------------------------
                // BASIC INFORMATION
                // -------------------------------------------------

                normalizedSymbol,

                normalizedSymbol,
                "Unknown",
                // -------------------------------------------------
                // MARKET DATA
                // -------------------------------------------------

                livePrice,

                technical.getCurrentPrice(),

                previousClose,

                changePercent,

                quote.getOpen(),

                quote.getHigh(),

                quote.getLow(),

                // -------------------------------------------------
                // VOLUME
                // -------------------------------------------------

                technical.getCurrentVolume(),

                technical.getAverageVolume20(),

                technical.getVolumeRatio(),

                // -------------------------------------------------
                // MOVING AVERAGES
                // -------------------------------------------------

                technical.getSma20(),

                technical.getSma50(),

                technical.getEma20(),

                // -------------------------------------------------
                // RSI
                // -------------------------------------------------

                technical.getRsi14(),

                // -------------------------------------------------
                // MACD
                // -------------------------------------------------

                technical.getMacd(),

                technical.getMacdSignal(),

                technical.getMacdHistogram(),

                // -------------------------------------------------
                // STATUS
                // -------------------------------------------------

                momentum,

                technical.getVolumeStatus(),

                technical.getTrend(),

                technical.getRsiStatus(),

                macdStatus,

                // -------------------------------------------------
                // SCORE
                // -------------------------------------------------

                score.total,

                score.momentumPoints,

                score.volumePoints,

                score.trendPoints,

                score.rsiPoints,

                score.macdPoints,

                score.priceStrengthPoints
        );
    }

    // =========================================================
    // MOMENTUM
    // =========================================================

    private String determineMomentum(
            double changePercent,
            double rsi,
            double macdHistogram) {

        if (changePercent >= 2
                && rsi >= 55
                && macdHistogram > 0) {

            return "Strong Positive";
        }

        if (changePercent > 0
                && rsi >= 50
                && macdHistogram >= 0) {

            return "Positive";
        }

        if (changePercent <= -2
                && rsi <= 45
                && macdHistogram < 0) {

            return "Strong Negative";
        }

        if (changePercent < 0
                && rsi <= 50
                && macdHistogram <= 0) {

            return "Negative";
        }

        return "Neutral";
    }

    // =========================================================
    // MACD STATUS
    // =========================================================

    private String determineMacdStatus(
            double macd,
            double signal,
            double histogram) {

        if (macd > signal
                && histogram > 0) {

            return "Bullish";
        }

        if (macd < signal
                && histogram < 0) {

            return "Bearish";
        }

        return "Neutral";
    }

    // =========================================================
    // SCORE ENGINE
    // =========================================================

    private ScoreResult calculateScore(
            double price,
            TechnicalIndicators technical,
            double changePercent) {

        double momentumPoints = 0.0;
        double volumePoints = 0.0;
        double trendPoints = 0.0;
        double rsiPoints = 0.0;
        double macdPoints = 0.0;
        double priceStrengthPoints = 0.0;

        // =====================================================
        // 1. MOMENTUM - 20 POINTS
        // =====================================================

        if (changePercent >= 2) {

            momentumPoints = 20;

        } else if (changePercent >= 1) {

            momentumPoints = 15;

        } else if (changePercent > 0) {

            momentumPoints = 10;

        } else if (changePercent <= -2) {

            momentumPoints = 0;

        } else if (changePercent < 0) {

            momentumPoints = 5;

        } else {

            momentumPoints = 8;
        }

        // =====================================================
        // 2. VOLUME - 15 POINTS
        // =====================================================

        double volumeRatio =
                technical.getVolumeRatio();

        if (volumeRatio >= 2.0) {

            volumePoints = 15;

        } else if (volumeRatio >= 1.5) {

            volumePoints = 12;

        } else if (volumeRatio >= 1.0) {

            volumePoints = 9;

        } else if (volumeRatio >= 0.75) {

            volumePoints = 6;

        } else {

            volumePoints = 3;
        }

        // =====================================================
        // 3. TREND - 25 POINTS
        // =====================================================

        String trend =
                technical.getTrend();

        if ("Strong Bullish".equals(trend)) {

            trendPoints = 25;

        } else if ("Bullish".equals(trend)) {

            trendPoints = 20;

        } else if ("Neutral".equals(trend)) {

            trendPoints = 12;

        } else if ("Bearish".equals(trend)) {

            trendPoints = 7;

        } else if ("Strong Bearish".equals(trend)) {

            trendPoints = 2;

        } else {

            trendPoints = 0;
        }

        // =====================================================
        // 4. RSI - 15 POINTS
        // =====================================================

        double rsi =
                technical.getRsi14();

        if (rsi >= 55
                && rsi < 70) {

            rsiPoints = 15;

        } else if (rsi >= 50
                && rsi < 55) {

            rsiPoints = 12;

        } else if (rsi >= 45
                && rsi < 50) {

            rsiPoints = 10;

        } else if (rsi >= 30
                && rsi < 45) {

            rsiPoints = 7;

        } else if (rsi < 30) {

            rsiPoints = 5;

        } else {

            // RSI >= 70
            // Do not automatically treat overbought
            // as bullish.

            rsiPoints = 8;
        }

        // =====================================================
        // 5. MACD - 15 POINTS
        // =====================================================

        double macd =
                technical.getMacd();

        double signal =
                technical.getMacdSignal();

        double histogram =
                technical.getMacdHistogram();

        if (macd > signal
                && histogram > 0) {

            macdPoints = 15;

        } else if (macd > signal) {

            macdPoints = 11;

        } else if (macd < signal
                && histogram < 0) {

            macdPoints = 4;

        } else {

            macdPoints = 8;
        }

        // =====================================================
        // 6. PRICE STRENGTH - 10 POINTS
        // =====================================================

        double sma20 =
                technical.getSma20();

        double sma50 =
                technical.getSma50();

        double ema20 =
                technical.getEma20();

        if (price > sma20
                && price > sma50
                && price > ema20) {

            priceStrengthPoints = 10;

        } else if (price > sma50) {

            priceStrengthPoints = 7;

        } else if (price > sma20) {

            priceStrengthPoints = 5;

        } else {

            priceStrengthPoints = 2;
        }

        // =====================================================
        // TOTAL SCORE
        // =====================================================

        double total =
                momentumPoints
                        + volumePoints
                        + trendPoints
                        + rsiPoints
                        + macdPoints
                        + priceStrengthPoints;

        // =====================================================
        // SAFETY CLAMP
        // =====================================================

        total =
                Math.max(
                        0,
                        Math.min(
                                total,
                                100
                        )
                );

        return new ScoreResult(
                total,
                momentumPoints,
                volumePoints,
                trendPoints,
                rsiPoints,
                macdPoints,
                priceStrengthPoints
        );
    }

    // =========================================================
    // INTERNAL SCORE RESULT
    // =========================================================

    private static class ScoreResult {

        private final double total;

        private final double momentumPoints;

        private final double volumePoints;

        private final double trendPoints;

        private final double rsiPoints;

        private final double macdPoints;

        private final double priceStrengthPoints;

        private ScoreResult(
                double total,
                double momentumPoints,
                double volumePoints,
                double trendPoints,
                double rsiPoints,
                double macdPoints,
                double priceStrengthPoints) {

            this.total = total;

            this.momentumPoints =
                    momentumPoints;

            this.volumePoints =
                    volumePoints;

            this.trendPoints =
                    trendPoints;

            this.rsiPoints =
                    rsiPoints;

            this.macdPoints =
                    macdPoints;

            this.priceStrengthPoints =
                    priceStrengthPoints;
        }
    }
}