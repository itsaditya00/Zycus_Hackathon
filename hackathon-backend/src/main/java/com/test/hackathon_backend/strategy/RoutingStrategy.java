package com.test.hackathon_backend.strategy;

import com.test.hackathon_backend.domain.Agent;
import com.test.hackathon_backend.domain.Order;
import com.test.hackathon_backend.domain.ReassignmentSuggestion;
import com.test.hackathon_backend.domain.TriggerReason;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface RoutingStrategy {
    ReassignmentSuggestion suggestReassignment(Order order, List<Agent> availableAgents, TriggerReason reason);
    String getStrategyName();
    
    default void streamReassignmentReasoning(Order order, List<Agent> availableAgents, TriggerReason reason, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().data("Streaming not supported for this strategy."));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}