package com.test.hackathon_backend.strategy;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import com.test.hackathon_backend.domain.Order;
import com.test.hackathon_backend.domain.ReassignmentSuggestion;
import com.test.hackathon_backend.domain.TriggerReason;
import com.test.hackathon_backend.domain.Agent;

@Component
public class RoutingEngineContext {

    private final Map<String, RoutingStrategy> strategies = new ConcurrentHashMap<>();
    private volatile String activeStrategyName = "AI_POWERED";

    public RoutingEngineContext(Map<String, RoutingStrategy> strategyMap) {
        strategyMap.forEach((key, val) -> this.strategies.put(val.getStrategyName(), val));
    }

    public synchronized void setActiveStrategy(String strategyName) {
        if (!strategies.containsKey(strategyName)) {
            throw new IllegalArgumentException("Unknown routing strategy payload constraint identity target: " + strategyName);
        }
        this.activeStrategyName = strategyName;
    }

    public RoutingStrategy getActiveStrategy() {
        return strategies.get(activeStrategyName);
    }

    public ReassignmentSuggestion executeStrategy(Order order, List<Agent> availableAgents, TriggerReason reason) {
        RoutingStrategy strategy = getActiveStrategy();
        if (strategy == null) {
            throw new IllegalStateException("No active routing strategy configured.");
        }
        // 4. Correctly invoke the actual interface method signature
        return strategy.suggestReassignment(order, availableAgents, reason);
    }
}