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
    private volatile String activeStrategyName = "AI_POWERED"; // Default fallback configuration

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

    // NEW METHOD: Resolves the request parameter, gracefully reverting to AI if unprovided or missing
    public ReassignmentSuggestion executeStrategyWithOverride(Order order, List<Agent> availableAgents, TriggerReason reason, String strategyOverride) {
        String targetStrategy = (strategyOverride == null || strategyOverride.isBlank()) 
                ? activeStrategyName 
                : strategyOverride.toUpperCase();

        RoutingStrategy strategy = strategies.get(targetStrategy);
        
        // Secondary safety loop if the provided string doesn't match a valid component name
        if (strategy == null) {
            strategy = getActiveStrategy();
        }
        
        if (strategy == null) {
            throw new IllegalStateException("No active or fallback routing strategy configured.");
        }
        
        return strategy.suggestReassignment(order, availableAgents, reason);
    }

    // Keep the original fallback mapping signature intact for backwards compatibility/internal triggers
    public ReassignmentSuggestion executeStrategy(Order order, List<Agent> availableAgents, TriggerReason reason) {
        return executeStrategyWithOverride(order, availableAgents, reason, null);
    }

    public RoutingStrategy resolveStrategy(String strategyOverride) {
        String targetStrategy = (strategyOverride == null || strategyOverride.isBlank()) 
                ? activeStrategyName 
                : strategyOverride.toUpperCase();

        RoutingStrategy strategy = strategies.get(targetStrategy);
        return (strategy != null) ? strategy : getActiveStrategy();
    }
}