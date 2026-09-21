package com.ram.ai.stockagent.agent;

import com.ram.ai.stockagent.market.Candle;
import com.ram.ai.stockagent.market.MarketDataProvider;
import com.ram.ai.stockagent.market.StockQuote;
import com.ram.ai.stockagent.model.StockAnalysis;
import com.ram.ai.stockagent.screener.StockScreenerService;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MarketDecisionService {

    private final MarketDataProvider marketDataProvider;
    private final StockScreenerService stockScreenerService;

    public MarketDecisionService(
            MarketDataProvider marketDataProvider,
            StockScreenerService stockScreenerService) {

        this.marketDataProvider = marketDataProvider;
        this.stockScreenerService = stockScreenerService;
    }

    /**
     * Finds candidate stocks using the existing Java analysis engine,
     * then enriches them with current Angel One market data.
     *
     * Existing database analysis is NOT replaced.
     */
    public List<StockMarketDecision> findBestStocks(
            String sector,
            int limit) {

        if (limit <= 0) {
            limit = 5;
        }

        /*
         * STEP 1:
         * Use the existing screener.
         *
         * This preserves the current application's
         * database + StockAnalysisService functionality.
         */
        List<StockAnalysis> candidates =
                stockScreenerService.screenStocks(
                        sector,
                        Math.max(limit * 3, 10)
                );

        List<StockMarketDecision> decisions =
                new ArrayList<>();

        /*
         * STEP 2:
         * For every candidate, retrieve LIVE Angel One data.
         */
        for (StockAnalysis analysis : candidates) {

            if (analysis == null
                    || analysis.getSymbol() == null
                    || analysis.getSymbol().trim().isEmpty()) {

                continue;
            }

            String symbol =
                    analysis.getSymbol()
                            .trim()
                            .toUpperCase();

            try {

                StockQuote quote =
                        marketDataProvider.getQuote(symbol);

                if (quote == null || !quote.isAvailable()) {
                    continue;
                }

                /*
                 * STEP 3:
                 * Retrieve recent historical candles.
                 *
                 * We deliberately keep the time window moderate
                 * so this service does not overload the broker API.
                 */
                LocalDateTime to =
                        LocalDateTime.now();

                LocalDateTime from =
                        to.minusDays(30);

                List<Candle> candles =
                        marketDataProvider.getHistoricalData(
                                symbol,
                                "ONE_DAY",
                                from,
                                to
                        );

                /*
                 * STEP 4:
                 * Create a market decision object.
                 *
                 * The actual AI/LLM reasoning will consume this
                 * structured information later.
                 */
                StockMarketDecision decision =
                        buildDecision(
                                analysis,
                                quote,
                                candles
                        );

                decisions.add(decision);

            } catch (Exception e) {

                System.err.println(
                        "Market analysis failed for "
                                + symbol
                                + ": "
                                + e.getMessage()
                );
            }
        }

        /*
         * Rank using the combined market score.
         */
        decisions.sort(
                Comparator.comparingDouble(
                        StockMarketDecision::getMarketScore
                ).reversed()
        );

        if (decisions.size() > limit) {

            return new ArrayList<>(
                    decisions.subList(0, limit)
            );
        }

        return decisions;
    }

    private StockMarketDecision buildDecision(
            StockAnalysis analysis,
            StockQuote quote,
            List<Candle> candles) {

        double trendScore =
                calculateTrendScore(candles);

        double pricePositionScore =
                calculatePricePositionScore(
                        quote,
                        candles
                );

        double existingScore =
                analysis.getScore() == null
                        ? 0.0
                        : analysis.getScore();

        /*
         * Existing Java analysis remains important.
         *
         * Live market information is additional intelligence.
         */
        double marketScore =
                (existingScore * 0.50)
                        + (trendScore * 0.30)
                        + (pricePositionScore * 0.20);

        StockMarketDecision decision =
                new StockMarketDecision();

        decision.setSymbol(
                quote.getSymbol()
        );

        decision.setTradingSymbol(
                quote.getTradingSymbol()
        );

        decision.setLtp(
                quote.getLtp()
        );

        decision.setOpen(
                quote.getOpen()
        );

        decision.setHigh(
                quote.getHigh()
        );

        decision.setLow(
                quote.getLow()
        );

        decision.setPreviousClose(
                quote.getClose()
        );

        decision.setExistingScore(
                existingScore
        );

        decision.setTrendScore(
                trendScore
        );

        decision.setPricePositionScore(
                pricePositionScore
        );

        decision.setMarketScore(
                marketScore
        );

        decision.setHistoricalDataAvailable(
                candles != null && !candles.isEmpty()
        );

        return decision;
    }

    private double calculateTrendScore(
            List<Candle> candles) {

        if (candles == null || candles.size() < 2) {
            return 0.0;
        }

        Candle first =
                candles.get(0);

        Candle last =
                candles.get(candles.size() - 1);

        if (first == null
                || last == null
                || first.getClose() <= 0) {

            return 0.0;
        }

        double change =
                ((last.getClose()
                        - first.getClose())
                        / first.getClose())
                        * 100.0;

        /*
         * Normalize approximately to 0-100.
         */
        double score =
                50.0 + (change * 5.0);

        return Math.max(
                0.0,
                Math.min(100.0, score)
        );
    }

    private double calculatePricePositionScore(
            StockQuote quote,
            List<Candle> candles) {

        if (quote == null
                || candles == null
                || candles.isEmpty()) {

            return 50.0;
        }

        double highest =
                candles.stream()
                        .filter(c -> c != null)
                        .mapToDouble(Candle::getHigh)
                        .max()
                        .orElse(quote.getHigh());

        double lowest =
                candles.stream()
                        .filter(c -> c != null)
                        .mapToDouble(Candle::getLow)
                        .min()
                        .orElse(quote.getLow());

        if (highest <= lowest) {
            return 50.0;
        }

        double position =
                (quote.getLtp() - lowest)
                        / (highest - lowest);

        /*
         * Middle/upper-middle price positioning
         * receives a neutral-to-positive score.
         */
        double score =
                100.0 - Math.abs(
                        (position * 100.0) - 60.0
                );

        return Math.max(
                0.0,
                Math.min(100.0, score)
        );
    }
}