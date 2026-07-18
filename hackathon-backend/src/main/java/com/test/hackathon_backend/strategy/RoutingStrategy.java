package com.test.hackathon_backend.strategy;

import com.test.hackathon_backend.domain.Agent;
import com.test.hackathon_backend.domain.Order;
import com.test.hackathon_backend.domain.ReassignmentSuggestion;
import com.test.hackathon_backend.domain.TriggerReason;
import java.util.List;

public interface RoutingStrategy {
    ReassignmentSuggestion suggestReassignment(Order order, List<Agent> availableAgents, TriggerReason reason);
    String getStrategyName();
    
}