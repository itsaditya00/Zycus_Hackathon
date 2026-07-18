package com.test.hackathon_backend.strategy;

import com.test.hackathon_backend.domain.*;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
// import java.util.stream.Collectors;

@Component("RULE_BASED")
public class RuleBasedRoutingStrategy implements RoutingStrategy {
    
    @Override
    public ReassignmentSuggestion suggestReassignment(Order order, List<Agent> availableAgents, TriggerReason reason) {
        Agent bestAgent = availableAgents.stream()
            .min(Comparator.comparingInt(Agent::getActiveOrderCount))
            .orElseThrow(() -> new IllegalStateException("No available agents detected in system state."));

        return ReassignmentSuggestion.builder()
            .id(UUID.randomUUID().toString())
            .orderId(order.getId())
            .recommendedAgentId(bestAgent.getId())
            .confidenceScore(0.5) // Standard baseline for deterministic defaults
            .reasoning("Rule-based Strategy fallback: Selected agent " + bestAgent.getName() + " due to lowest relative active workload load profile.")
            .status(SuggestionStatus.PENDING)
            .triggerReason(reason)
            .build();
    }

    @Override
    public String getStrategyName() {
        return "RULE_BASED";
    }

    // @Override
    // public List<Agent> executeStrategy(Order order, List<Agent> availableAgents) {
    //     return availableAgents.stream()
    //             .sorted(Comparator.comparingInt(Agent::getActiveOrderCount))
    //             .collect(Collectors.toList());
    // }
}