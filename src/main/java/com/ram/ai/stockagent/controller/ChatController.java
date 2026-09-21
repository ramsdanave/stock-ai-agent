package com.ram.ai.stockagent.controller;

import com.ram.ai.stockagent.agent.MarketAiCoordinator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final MarketAiCoordinator marketAiCoordinator;

    public ChatController(
            MarketAiCoordinator marketAiCoordinator) {

        this.marketAiCoordinator = marketAiCoordinator;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        try {

            String answer =
                    marketAiCoordinator.process(
                            request.getMessage()
                    );

            return new ChatResponse(answer);

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "CHAT ERROR: "
                            + e.getClass().getName()
                            + " : "
                            + e.getMessage(),
                    e
            );
        }
    }
}