package com.ram.ai.stockagent.controller;

import com.ram.ai.stockagent.analysis.StockAnalysisService;
import com.ram.ai.stockagent.model.StockAnalysis;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
public class StockAnalysisController {

    private final StockAnalysisService stockAnalysisService;

    public StockAnalysisController(
            StockAnalysisService stockAnalysisService) {

        this.stockAnalysisService = stockAnalysisService;
    }

    @GetMapping("/{symbol}")
    public StockAnalysis analyze(
            @PathVariable String symbol) {

        return stockAnalysisService.analyzeStock(symbol);
    }
}