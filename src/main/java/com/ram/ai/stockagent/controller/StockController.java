package com.ram.ai.stockagent.controller;

import com.ram.ai.stockagent.model.Stock;
import com.ram.ai.stockagent.service.StockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public List<Stock> getAllStocks() {
        return stockService.getAllStocks();
    }

    @GetMapping("/sector/{sector}")
    public List<Stock> getBySector(
            @PathVariable String sector) {

        return stockService.getStocksBySector(sector);
    }

    @GetMapping("/{symbol}")
    public Stock getBySymbol(
            @PathVariable String symbol) {

        return stockService.getStockBySymbol(symbol);
    }
}