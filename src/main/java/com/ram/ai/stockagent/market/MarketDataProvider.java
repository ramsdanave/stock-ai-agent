package com.ram.ai.stockagent.market;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketDataProvider {

    StockQuote getQuote(String symbol);

    List<Candle> getHistoricalData(
            String symbol,
            String interval,
            LocalDateTime from,
            LocalDateTime to
    );
}