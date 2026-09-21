package com.ram.ai.stockagent.agent;

import com.ram.ai.stockagent.model.StockAnalysis;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

@Service
public class StockRiskService {

    public RiskAssessment assess(StockAnalysis analysis) {

        if (analysis == null) {

            return new RiskAssessment(
                    "DO NOT BUY",
                    "VERY HIGH",
                    "Stock analysis data is unavailable.",
                    10,
                    "No recommendation should be made without valid analysis data."
            );
        }

        double score =
                getDouble(analysis, "getScore");

        double rsi =
                getDouble(analysis, "getRsi");

        double volumeRatio =
                getDouble(analysis, "getVolumeRatio");

        String trend =
                getString(analysis, "getTrend");

        String momentum =
                getString(analysis, "getMomentum");

        String macd =
                getString(analysis, "getMacdStatus");

        int riskPoints = 0;

        StringBuilder warnings =
                new StringBuilder();

        // =====================================================
        // SCORE
        // =====================================================

        if (score < 45) {

            riskPoints += 3;

            warnings.append(
                    "Technical score is weak. "
            );

        } else if (score < 60) {

            riskPoints += 2;

            warnings.append(
                    "Technical score is moderate. "
            );

        } else if (score < 75) {

            riskPoints += 1;

            warnings.append(
                    "Technical score is positive but "
                            + "not exceptionally strong. "
            );
        }

        // =====================================================
        // RSI
        // =====================================================

        if (rsi >= 70) {

            riskPoints += 3;

            warnings.append(
                    "RSI is overbought. "
            );

        } else if (rsi >= 65) {

            riskPoints += 2;

            warnings.append(
                    "RSI is elevated. "
            );

        } else if (rsi > 0 && rsi < 30) {

            riskPoints += 2;

            warnings.append(
                    "RSI is oversold and reversal risk "
                            + "is elevated. "
            );
        }

        // =====================================================
        // TREND
        // =====================================================

        if (contains(
                trend,
                "BEARISH"
        )) {

            riskPoints += 3;

            warnings.append(
                    "Trend is bearish. "
            );

        } else if (contains(
                trend,
                "NEUTRAL"
        )) {

            riskPoints += 1;

            warnings.append(
                    "Trend confirmation is neutral. "
            );
        }

        // =====================================================
        // MOMENTUM
        // =====================================================

        if (contains(
                momentum,
                "BEARISH"
        )
                || contains(
                momentum,
                "NEGATIVE"
        )) {

            riskPoints += 3;

            warnings.append(
                    "Momentum is negative. "
            );

        } else if (contains(
                momentum,
                "NEUTRAL"
        )) {

            riskPoints += 1;

            warnings.append(
                    "Momentum is neutral. "
            );
        }

        // =====================================================
        // MACD
        // =====================================================

        if (contains(
                macd,
                "BEARISH"
        )) {

            riskPoints += 3;

            warnings.append(
                    "MACD is bearish. "
            );

        } else if (contains(
                macd,
                "NEUTRAL"
        )) {

            riskPoints += 1;

            warnings.append(
                    "MACD confirmation is neutral. "
            );
        }

        // =====================================================
        // VOLUME
        // =====================================================

        if (volumeRatio > 0
                && volumeRatio < 0.70) {

            riskPoints += 2;

            warnings.append(
                    "Volume is significantly below average. "
            );

        } else if (volumeRatio > 0
                && volumeRatio < 1.00) {

            riskPoints += 1;

            warnings.append(
                    "Volume is below average. "
            );
        }

        // =====================================================
        // FINAL RISK
        // =====================================================

        String riskLevel;

        String recommendationGuard;

        if (riskPoints >= 9) {

            riskLevel = "VERY HIGH";

            recommendationGuard =
                    "DO NOT BUY";

        } else if (riskPoints >= 6) {

            riskLevel = "HIGH";

            recommendationGuard =
                    "WAIT FOR CONFIRMATION";

        } else if (riskPoints >= 3) {

            riskLevel = "MEDIUM";

            recommendationGuard =
                    "WATCH";

        } else {

            riskLevel = "LOW";

            recommendationGuard =
                    "ELIGIBLE FOR FURTHER REVIEW";
        }

        String reason;

        if (warnings.length() == 0) {

            reason =
                    "No major technical risk flags "
                            + "were identified.";

        } else {

            reason =
                    warnings.toString().trim();
        }

        return new RiskAssessment(
                recommendationGuard,
                riskLevel,
                reason,
                riskPoints,
                "A high technical score alone does not "
                        + "automatically justify a BUY decision."
        );
    }

    // =========================================================
    // REFLECTION HELPERS
    // =========================================================

    private double getDouble(
            StockAnalysis analysis,
            String methodName) {

        try {

            Method method =
                    analysis.getClass()
                            .getMethod(
                                    methodName
                            );

            Object value =
                    method.invoke(
                            analysis
                    );

            if (value instanceof Number) {

                return ((Number) value)
                        .doubleValue();
            }

        } catch (Exception ignored) {

            // Optional field not available.
        }

        return 0.0;
    }

    private String getString(
            StockAnalysis analysis,
            String methodName) {

        try {

            Method method =
                    analysis.getClass()
                            .getMethod(
                                    methodName
                            );

            Object value =
                    method.invoke(
                            analysis
                    );

            if (value != null) {

                return value.toString();
            }

        } catch (Exception ignored) {

            // Optional field not available.
        }

        return "";
    }

    private boolean contains(
            String value,
            String search) {

        if (value == null
                || search == null) {

            return false;
        }

        return value
                .toUpperCase()
                .contains(
                        search.toUpperCase()
                );
    }

    // =========================================================
    // RESULT
    // =========================================================

    public static class RiskAssessment {

        private String recommendationGuard;

        private String riskLevel;

        private String riskReason;

        private int riskPoints;

        private String systemWarning;

        public RiskAssessment() {
        }

        public RiskAssessment(
                String recommendationGuard,
                String riskLevel,
                String riskReason,
                int riskPoints,
                String systemWarning) {

            this.recommendationGuard =
                    recommendationGuard;

            this.riskLevel =
                    riskLevel;

            this.riskReason =
                    riskReason;

            this.riskPoints =
                    riskPoints;

            this.systemWarning =
                    systemWarning;
        }

        public String getRecommendationGuard() {
            return recommendationGuard;
        }

        public void setRecommendationGuard(
                String recommendationGuard) {

            this.recommendationGuard =
                    recommendationGuard;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(
                String riskLevel) {

            this.riskLevel =
                    riskLevel;
        }

        public String getRiskReason() {
            return riskReason;
        }

        public void setRiskReason(
                String riskReason) {

            this.riskReason =
                    riskReason;
        }

        public int getRiskPoints() {
            return riskPoints;
        }

        public void setRiskPoints(
                int riskPoints) {

            this.riskPoints =
                    riskPoints;
        }

        public String getSystemWarning() {
            return systemWarning;
        }

        public void setSystemWarning(
                String systemWarning) {

            this.systemWarning =
                    systemWarning;
        }
    }
}