package com.ram.ai.stockagent.agent;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentTestController {

    private final MarketAiCoordinator coordinator;

    public AgentTestController(MarketAiCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @GetMapping("/test")
    public String test(@RequestParam String query) {
        return coordinator.process(query);
    }
}