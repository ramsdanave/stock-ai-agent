package com.ram.ai.stockagent.controller;

import com.ram.ai.stockagent.market.MarketDataProvider;
import com.ram.ai.stockagent.market.StockQuote;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market")
public class MarketTestController {

    private final MarketDataProvider marketDataProvider;

    public MarketTestController(
            MarketDataProvider marketDataProvider) {

        this.marketDataProvider = marketDataProvider;
    }

    @GetMapping("/{symbol}")
    public StockQuote getQuote(
            @PathVariable String symbol) {

        return marketDataProvider.getQuote(symbol);
    }
}