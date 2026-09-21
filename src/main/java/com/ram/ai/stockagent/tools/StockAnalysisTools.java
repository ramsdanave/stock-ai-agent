package com.ram.ai.stockagent.tools;

import com.ram.ai.stockagent.agent.StockRecommendationService;
import com.ram.ai.stockagent.analysis.StockAnalysisService;
import com.ram.ai.stockagent.market.MarketDataProvider;
import com.ram.ai.stockagent.market.StockQuote;
import com.ram.ai.stockagent.model.StockAnalysis;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class StockAnalysisTools {

    private final StockAnalysisService stockAnalysisService;

    private final MarketDataProvider marketDataProvider;

    private final StockRecommendationService stockRecommendationService;

    public StockAnalysisTools(
            StockAnalysisService stockAnalysisService,
            MarketDataProvider marketDataProvider,
            StockRecommendationService stockRecommendationService) {

        this.stockAnalysisService =
                stockAnalysisService;

        this.marketDataProvider =
                marketDataProvider;

        this.stockRecommendationService =
                stockRecommendationService;
    }

    // =========================================================
    // TOOL 1: LIVE STOCK QUOTE
    // =========================================================

    @Tool(
        name = "getLiveStockQuote",
        description = """
            Retrieves LIVE market data from the configured
            Angel One SmartAPI market-data provider.

            Use this tool for:

            - current price
            - live price
            - LTP
            - today's open
            - today's high
            - today's low
            - previous close
            - live market data

            IMPORTANT:

            This is the authoritative tool for current
            market prices.

            Never invent a price.

            Never use database prices as live prices.

            If available is false, live market data was not
            successfully retrieved.
            """
    )
    public StockQuote getLiveStockQuote(String symbol) {

        if (symbol == null
                || symbol.trim().isEmpty()) {

            StockQuote result =
                    new StockQuote(
                            "",
                            "NSE",
                            "",
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0
                    );

            result.setAvailable(false);

            result.setErrorMessage(
                    "No stock symbol was provided."
            );

            return result;
        }

        symbol =
                symbol.trim().toUpperCase();

        try {

            StockQuote quote =
                    marketDataProvider.getQuote(symbol);

            if (quote == null) {

                StockQuote result =
                        new StockQuote(
                                symbol,
                                "NSE",
                                symbol,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                0.0
                        );

                result.setAvailable(false);

                result.setErrorMessage(
                        "Live market data was not available for "
                                + symbol
                );

                return result;
            }

            quote.setAvailable(true);

            quote.setErrorMessage(null);

            return quote;

        } catch (Exception e) {

            System.err.println(
                    "Live quote lookup failed for "
                            + symbol
                            + ": "
                            + e.getMessage()
            );

            StockQuote result =
                    new StockQuote(
                            symbol,
                            "NSE",
                            symbol,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0
                    );

            result.setAvailable(false);

            result.setErrorMessage(
                    "NSE live market data is not available for "
                            + symbol
                            + ". "
                            + e.getMessage()
            );

            return result;
        }
    }

    // =========================================================
    // TOOL 2: INDIVIDUAL STOCK ANALYSIS
    // =========================================================

    @Tool(
        name = "analyzeStock",
        description = """
            Performs technical analysis of a specific stock
            using the application's Java analysis engine.

            Use this tool for a SPECIFIC stock symbol.

            It can provide:

            - technical score
            - trend
            - momentum
            - RSI
            - MACD
            - volume
            - technical strength

            The Java analysis engine calculates the values.

            Do not invent or manually calculate the score.
            """
    )
    
    	public StockAnalysis analyzeStock(String symbol) {

    	    if (symbol == null || symbol.trim().isEmpty()) {
    	        return null;
    	    }

    	    String normalizedSymbol =
    	            symbol.trim().toUpperCase();

    	    try {

    	        return stockAnalysisService.analyzeStock(
    	                normalizedSymbol
    	        );

    	    } catch (Exception e) {

    	        System.err.println(
    	                "TECHNICAL ANALYSIS FAILED FOR "
    	                        + normalizedSymbol
    	                        + ": "
    	                        + e.getMessage()
    	        );

    	        return null;
    	    }
    	}

    // =========================================================
    // TOOL 3: ONLINE STOCK RECOMMENDATION
    // =========================================================

    @Tool(
        name = "recommendStocksOnline",
        description = """
            PRIMARY TOOL FOR STOCK RECOMMENDATIONS
            AND SECTOR SCREENING.

            Use this tool whenever the user asks:

            - best stock
            - best stock right now
            - strongest stock
            - best candidate
            - stock to consider
            - stock recommendation
            - best IT stock
            - best banking stock
            - best pharma stock
            - best auto stock
            - best FMCG stock
            - strongest banking stock
            - strongest pharma stock
            - top stocks in a sector
            - which stock should I consider
            - compare stocks within a sector

            IMPORTANT:

            Stock discovery is performed ONLINE.

            DO NOT use the application database to discover
            sector stocks.

            DO NOT use database sector screening for
            recommendations.

            Online candidates are analyzed using the
            application's technical analysis engine.

            The result contains:

            - online candidates
            - technical scores
            - ranking
            - best candidate
            - decision
            - confidence
            - reasoning

            Supported sectors include:

            IT
            BANKING
            PHARMA
            AUTO
            FMCG
            METAL
            REALTY
            MEDIA
            ENERGY
            PSU BANK
            PRIVATE BANK
            FINANCE
            ALL
            """
    )
    public StockRecommendationService.StockRecommendation
            recommendStocksOnline(
                    String sector,
                    Integer limit) {

        if (sector == null
                || sector.trim().isEmpty()) {

            sector = "ALL";
        }

        int actualLimit =
                limit == null
                        ? 5
                        : limit;

        if (actualLimit <= 0) {
            actualLimit = 5;
        }

        return stockRecommendationService.recommend(
                sector.trim().toUpperCase(),
                actualLimit
        );
    }

    // =========================================================
    // TOOL 4: ONLINE STOCK COMPARISON
    // =========================================================

    @Tool(
    	    name = "compareStocksOnline",
    	    description = """
    	        Compares specific stock symbols using online market
    	        data and technical analysis.

    	        Analyze EVERY symbol provided by the user independently.

    	        Never remove a requested symbol simply because it is
    	        absent from a sector screening result.

    	        If analysis is unavailable for one symbol, continue
    	        analyzing the remaining symbols.
    	        """
    	)
    	public List<StockAnalysis> compareStocksOnline(
    	        List<String> symbols) {

    	    List<StockAnalysis> results =
    	            new java.util.ArrayList<>();

    	    if (symbols == null || symbols.isEmpty()) {
    	        return results;
    	    }

    	    for (String symbol : symbols) {

    	        if (symbol == null || symbol.trim().isEmpty()) {
    	            continue;
    	        }

    	        String cleanSymbol =
    	                symbol.trim().toUpperCase();

    	        try {

    	            StockQuote quote =
    	                    marketDataProvider.getQuote(cleanSymbol);

    	            if (quote == null) {

    	                System.err.println(
    	                        "No live quote returned for "
    	                                + cleanSymbol
    	                );

    	                continue;
    	            }

    	            StockAnalysis analysis =
    	                    stockAnalysisService.analyzeStock(
    	                            cleanSymbol
    	                    );

    	            if (analysis != null) {

    	                results.add(analysis);

    	            } else {

    	                System.err.println(
    	                        "Technical analysis returned null for "
    	                                + cleanSymbol
    	                );
    	            }

    	        } catch (Exception e) {

    	            System.err.println(
    	                    "Comparison failed for "
    	                            + cleanSymbol
    	                            + ": "
    	                            + e.getMessage()
    	            );
    	        }
    	    }

    	    results.sort(
    	            java.util.Comparator.comparing(
    	                    StockAnalysis::getScore,
    	                    java.util.Comparator.nullsLast(
    	                            java.util.Comparator.reverseOrder()
    	                    )
    	            )
    	    );

    	    return results;
    	}
}