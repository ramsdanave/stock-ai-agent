package com.ram.ai.stockagent.service;

import com.ram.ai.stockagent.model.Stock;
import com.ram.ai.stockagent.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public List<Stock> getStocksBySector(String sector) {
        return stockRepository.findBySector(sector);
    }

    public Stock getStockBySymbol(String symbol) {

        return stockRepository
                .findBySymbol(symbol.toUpperCase())
                .orElse(null);
    }
}