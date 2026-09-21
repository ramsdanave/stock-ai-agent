package com.ram.ai.stockagent.analysis;

import com.ram.ai.stockagent.market.Candle;
import com.ram.ai.stockagent.market.MarketDataProvider;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TechnicalAnalysisService {

    private final MarketDataProvider marketDataProvider;

    public TechnicalAnalysisService(
            MarketDataProvider marketDataProvider) {

        this.marketDataProvider = marketDataProvider;
    }

    // =========================================================
    // MAIN METHOD
    // =========================================================

    public TechnicalIndicators analyze(
            String symbol) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Stock symbol cannot be empty"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        // -----------------------------------------------------
        // Get 90 days of real Angel One historical data
        // -----------------------------------------------------

        LocalDateTime to =
                LocalDateTime.now();

        LocalDateTime from =
                to.minusDays(90);

        List<Candle> candles =
                marketDataProvider.getHistoricalData(
                        normalizedSymbol,
                        "ONE_DAY",
                        from,
                        to
                );

        if (candles == null || candles.isEmpty()) {

            throw new RuntimeException(
                    "No historical data available for "
                            + normalizedSymbol
            );
        }

        // Make sure candles are chronological.
        candles.sort(
                (a, b) ->
                        a.getTimestamp()
                                .compareTo(
                                        b.getTimestamp()
                                )
        );

        if (candles.size() < 50) {

            throw new RuntimeException(
                    "At least 50 trading candles are required "
                            + "for technical analysis. Available: "
                            + candles.size()
            );
        }

        // -----------------------------------------------------
        // Close prices
        // -----------------------------------------------------

        List<Double> closes =
                new ArrayList<>();

        for (Candle candle : candles) {
            closes.add(candle.getClose());
        }

        Candle latest =
                candles.get(
                        candles.size() - 1
                );

        double currentPrice =
                latest.getClose();

        // =====================================================
        // SMA 20
        // =====================================================

        double sma20 =
                calculateSma(
                        closes,
                        20
                );

        // =====================================================
        // SMA 50
        // =====================================================

        double sma50 =
                calculateSma(
                        closes,
                        50
                );

        // =====================================================
        // EMA 20
        // =====================================================

        double ema20 =
                calculateLatestEma(
                        closes,
                        20
                );

        // =====================================================
        // RSI 14
        // =====================================================

        double rsi14 =
                calculateRsi(
                        closes,
                        14
                );

        // =====================================================
        // MACD 12,26,9
        // =====================================================

        MacdResult macd =
                calculateMacd(
                        closes,
                        12,
                        26,
                        9
                );

        // =====================================================
        // VOLUME
        // =====================================================

        long currentVolume =
                latest.getVolume();

        double averageVolume20 =
                calculateAverageVolume(
                        candles,
                        20
                );

        double volumeRatio = 0.0;

        if (averageVolume20 > 0) {

            volumeRatio =
                    currentVolume
                            / averageVolume20;
        }

        // =====================================================
        // RSI STATUS
        // =====================================================

        String rsiStatus =
                classifyRsi(rsi14);

        // =====================================================
        // VOLUME STATUS
        // =====================================================

        String volumeStatus =
                classifyVolume(volumeRatio);

        // =====================================================
        // TREND
        // =====================================================

        String trend =
                determineTrend(
                        currentPrice,
                        sma20,
                        sma50,
                        ema20
                );

        return new TechnicalIndicators(
                normalizedSymbol,
                currentPrice,
                sma20,
                sma50,
                ema20,
                rsi14,
                macd.macd,
                macd.signal,
                macd.histogram,
                currentVolume,
                averageVolume20,
                volumeRatio,
                rsiStatus,
                volumeStatus,
                trend
        );
    }

    // =========================================================
    // SMA
    // =========================================================

    private double calculateSma(
            List<Double> values,
            int period) {

        if (values.size() < period) {

            throw new IllegalArgumentException(
                    "Not enough values for SMA " + period
            );
        }

        double sum = 0.0;

        int start =
                values.size() - period;

        for (int i = start;
             i < values.size();
             i++) {

            sum += values.get(i);
        }

        return sum / period;
    }

    // =========================================================
    // EMA
    // =========================================================

    private double calculateLatestEma(
            List<Double> values,
            int period) {

        if (values.size() < period) {

            throw new IllegalArgumentException(
                    "Not enough values for EMA "
                            + period
            );
        }

        // Start EMA with SMA of first period values.
        double ema = 0.0;

        for (int i = 0; i < period; i++) {
            ema += values.get(i);
        }

        ema /= period;

        double multiplier =
                2.0 / (period + 1);

        for (int i = period;
             i < values.size();
             i++) {

            ema =
                    (
                            (values.get(i) - ema)
                                    * multiplier
                    )
                            + ema;
        }

        return ema;
    }

    // =========================================================
    // RSI
    // =========================================================

    private double calculateRsi(
            List<Double> closes,
            int period) {

        if (closes.size() <= period) {

            throw new IllegalArgumentException(
                    "Not enough data for RSI "
                            + period
            );
        }

        double gainSum = 0.0;
        double lossSum = 0.0;

        // -----------------------------------------------------
        // Initial average gain/loss
        // -----------------------------------------------------

        for (int i = 1;
             i <= period;
             i++) {

            double change =
                    closes.get(i)
                            - closes.get(i - 1);

            if (change > 0) {

                gainSum += change;

            } else {

                lossSum += Math.abs(change);
            }
        }

        double averageGain =
                gainSum / period;

        double averageLoss =
                lossSum / period;

        // -----------------------------------------------------
        // Wilder's smoothing
        // -----------------------------------------------------

        for (int i = period + 1;
             i < closes.size();
             i++) {

            double change =
                    closes.get(i)
                            - closes.get(i - 1);

            double gain =
                    Math.max(
                            change,
                            0
                    );

            double loss =
                    Math.max(
                            -change,
                            0
                    );

            averageGain =
                    (
                            (averageGain * (period - 1))
                                    + gain
                    )
                            / period;

            averageLoss =
                    (
                            (averageLoss * (period - 1))
                                    + loss
                    )
                            / period;
        }

        if (averageLoss == 0) {
            return 100.0;
        }

        double relativeStrength =
                averageGain / averageLoss;

        return 100.0
                -
                (
                        100.0
                                /
                                (1.0 + relativeStrength)
                );
    }

    // =========================================================
    // MACD
    // =========================================================

    private MacdResult calculateMacd(
            List<Double> closes,
            int fastPeriod,
            int slowPeriod,
            int signalPeriod) {

        if (closes.size() < slowPeriod + signalPeriod) {

            throw new IllegalArgumentException(
                    "Not enough data for MACD"
            );
        }

        List<Double> macdValues =
                new ArrayList<>();

        double fastEma =
                calculateInitialEma(
                        closes,
                        fastPeriod
                );

        double slowEma =
                calculateInitialEma(
                        closes,
                        slowPeriod
                );

        double fastMultiplier =
                2.0
                        /
                        (fastPeriod + 1);

        double slowMultiplier =
                2.0
                        /
                        (slowPeriod + 1);

        // -----------------------------------------------------
        // Calculate EMA values from the slow-period starting
        // point.
        // -----------------------------------------------------

        for (int i = slowPeriod;
             i < closes.size();
             i++) {

            fastEma =
                    fastEma
                            +
                            (
                                    (
                                            closes.get(i)
                                                    - fastEma
                                    )
                                            * fastMultiplier
                            );

            slowEma =
                    slowEma
                            +
                            (
                                    (
                                            closes.get(i)
                                                    - slowEma
                                    )
                                            * slowMultiplier
                            );

            macdValues.add(
                    fastEma - slowEma
            );
        }

        if (macdValues.size() < signalPeriod) {

            throw new IllegalArgumentException(
                    "Not enough MACD values for signal line"
            );
        }

        double signal =
                calculateLatestEma(
                        macdValues,
                        signalPeriod
                );

        double macd =
                macdValues.get(
                        macdValues.size() - 1
                );

        double histogram =
                macd - signal;

        return new MacdResult(
                macd,
                signal,
                histogram
        );
    }

    // =========================================================
    // INITIAL EMA
    // =========================================================

    private double calculateInitialEma(
            List<Double> values,
            int period) {

        double sum = 0.0;

        for (int i = 0;
             i < period;
             i++) {

            sum += values.get(i);
        }

        return sum / period;
    }

    // =========================================================
    // AVERAGE VOLUME
    // =========================================================

    private double calculateAverageVolume(
            List<Candle> candles,
            int period) {

        if (candles.size() < period) {

            throw new IllegalArgumentException(
                    "Not enough candles for volume average"
            );
        }

        long total = 0;

        int start =
                candles.size() - period;

        for (int i = start;
             i < candles.size();
             i++) {

            total +=
                    candles.get(i)
                            .getVolume();
        }

        return (double) total / period;
    }

    // =========================================================
    // RSI CLASSIFICATION
    // =========================================================

    private String classifyRsi(
            double rsi) {

        if (rsi >= 70) {
            return "Overbought";

        } else if (rsi <= 30) {
            return "Oversold";

        } else if (rsi >= 55) {
            return "Bullish";

        } else if (rsi <= 45) {
            return "Bearish";

        } else {
            return "Neutral";
        }
    }

    // =========================================================
    // VOLUME CLASSIFICATION
    // =========================================================

    private String classifyVolume(
            double ratio) {

        if (ratio >= 2.0) {
            return "Very High";

        } else if (ratio >= 1.5) {
            return "High";

        } else if (ratio >= 1.0) {
            return "Normal";

        } else {
            return "Below Average";
        }
    }

    // =========================================================
    // TREND CLASSIFICATION
    // =========================================================

    private String determineTrend(
            double price,
            double sma20,
            double sma50,
            double ema20) {

        if (price > sma20
                && sma20 > sma50
                && price > ema20) {

            return "Strong Bullish";
        }

        if (price > sma20
                && price > sma50) {

            return "Bullish";
        }

        if (price < sma20
                && sma20 < sma50
                && price < ema20) {

            return "Strong Bearish";
        }

        if (price < sma20
                && price < sma50) {

            return "Bearish";
        }

        return "Neutral";
    }

    // =========================================================
    // MACD RESULT
    // =========================================================

    private static class MacdResult {

        private final double macd;

        private final double signal;

        private final double histogram;

        private MacdResult(
                double macd,
                double signal,
                double histogram) {

            this.macd = macd;
            this.signal = signal;
            this.histogram = histogram;
        }
    }
}