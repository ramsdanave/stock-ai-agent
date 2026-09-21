package com.ram.ai.stockagent.controller;

import com.ram.ai.stockagent.market.Candle;
import com.ram.ai.stockagent.market.MarketDataProvider;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/market")
public class HistoricalMarketTestController {

    private final MarketDataProvider marketDataProvider;

    public HistoricalMarketTestController(
            MarketDataProvider marketDataProvider) {

        this.marketDataProvider =
                marketDataProvider;
    }

    @GetMapping("/history/{symbol}")
    public List<Candle> getHistory(
            @PathVariable String symbol) {

        LocalDateTime to =
                LocalDateTime.now();

        LocalDateTime from =
                to.minusDays(90);

        return marketDataProvider.getHistoricalData(
                symbol,
                "ONE_DAY",
                from,
                to
        );
    }
}