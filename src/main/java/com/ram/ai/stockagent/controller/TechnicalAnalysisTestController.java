package com.ram.ai.stockagent.controller;

import com.ram.ai.stockagent.analysis.TechnicalAnalysisService;
import com.ram.ai.stockagent.analysis.TechnicalIndicators;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/technical")
public class TechnicalAnalysisTestController {

    private final TechnicalAnalysisService technicalAnalysisService;

    public TechnicalAnalysisTestController(
            TechnicalAnalysisService technicalAnalysisService) {

        this.technicalAnalysisService =
                technicalAnalysisService;
    }

    @GetMapping("/{symbol}")
    public TechnicalIndicators analyze(
            @PathVariable String symbol) {

        return technicalAnalysisService.analyze(
                symbol
        );
    }
}