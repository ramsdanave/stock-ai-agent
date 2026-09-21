package com.ram.ai.stockagent.screener;

import com.ram.ai.stockagent.analysis.StockAnalysisService;
import com.ram.ai.stockagent.model.Stock;
import com.ram.ai.stockagent.model.StockAnalysis;
import com.ram.ai.stockagent.repository.StockRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StockScreenerService {

    private final StockRepository stockRepository;

    private final StockAnalysisService stockAnalysisService;

    public StockScreenerService(
            StockRepository stockRepository,
            StockAnalysisService stockAnalysisService) {

        this.stockRepository = stockRepository;
        this.stockAnalysisService = stockAnalysisService;
    }

    public List<StockAnalysis> screenStocks(
            String sector,
            int limit) {

        // ---------------------------------------------------------
        // DEFAULT LIMIT
        // ---------------------------------------------------------

        if (limit <= 0) {
            limit = 5;
        }

        // ---------------------------------------------------------
        // GET STOCKS
        // ---------------------------------------------------------

        List<Stock> stocks;

        if (sector == null
                || sector.trim().isEmpty()
                || sector.equalsIgnoreCase("ALL")) {

            stocks = stockRepository.findAll();

        } else {

            stocks = stockRepository.findBySector(
                    sector.trim().toUpperCase()
            );
        }

        // ---------------------------------------------------------
        // SAFETY CHECK
        // ---------------------------------------------------------

        if (stocks == null || stocks.isEmpty()) {
            return new ArrayList<>();
        }

        // ---------------------------------------------------------
        // ANALYZE EVERY STOCK
        // ---------------------------------------------------------

        List<StockAnalysis> results =
                new ArrayList<>();

        for (Stock stock : stocks) {

            if (stock == null) {
                continue;
            }

            try {

                StockAnalysis analysis =
                        stockAnalysisService.analyze(stock);

                if (analysis != null) {
                    results.add(analysis);
                }

            } catch (Exception e) {

                System.err.println(
                        "Stock analysis failed for "
                                + getStockSymbol(stock)
                                + ": "
                                + e.getMessage()
                );
            }
        }

        // ---------------------------------------------------------
        // RANK BY EXISTING JAVA ANALYSIS SCORE
        // ---------------------------------------------------------

        results.sort(
                Comparator.comparing(
                        StockAnalysis::getScore,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        // ---------------------------------------------------------
        // RETURN TOP RESULTS
        // ---------------------------------------------------------

        if (results.size() > limit) {

            return new ArrayList<>(
                    results.subList(0, limit)
            );
        }

        return results;
    }

    // -------------------------------------------------------------
    // SAFE STOCK SYMBOL FOR LOGGING
    // -------------------------------------------------------------

    private String getStockSymbol(Stock stock) {

        try {

            if (stock == null) {
                return "UNKNOWN";
            }

            /*
             * Keep this method independent from the screening logic.
             * If Stock exposes getSymbol(), use it for logging.
             */

            return stock.getSymbol();

        } catch (Exception e) {

            return "UNKNOWN";
        }
    }
}