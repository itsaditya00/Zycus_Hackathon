package com.test.hackathon_backend.event;

import com.test.hackathon_backend.domain.*;
import com.test.hackathon_backend.repository.*;
import com.test.hackathon_backend.strategy.RoutingEngineContext;
import com.test.hackathon_backend.strategy.RoutingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class AgenticReplanListener {

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final ReassignmentSuggestionRepository suggestionRepository;
    private final RoutingEngineContext engineContext;
    private final RoutingStrategy ruleBasedFallback;

    public AgenticReplanListener(OrderRepository orderRepository,
                                 AgentRepository agentRepository,
                                 ReassignmentSuggestionRepository suggestionRepository,
                                 RoutingEngineContext engineContext,
                                 @Qualifier("RULE_BASED") RoutingStrategy ruleBasedFallback) {
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
        this.suggestionRepository = suggestionRepository;
        this.engineContext = engineContext;
        this.ruleBasedFallback = ruleBasedFallback;
    }

    @Async
    @Transactional // Added to ensure database transactional integrity during auto-assignment updates
    @EventListener
    public void handleAgentOffline(AgentOfflineEvent event) {
        log.info("Async Agentic Auto-Reassignment sequence initiated for Agent: {}", event.getAgentId());
        
        // 1. Fetch orders assigned to the offline agent
        List<Order> impactedOrders = orderRepository.findByAssignedAgentIdAndStatusNot(
            event.getAgentId(), OrderStatus.DELIVERED
        );

        if (impactedOrders.isEmpty()) {
            return;
        }

        // 2. Fetch backup candidates[cite: 27]
        List<Agent> availableAgents = agentRepository.findByStatusIn(
            Arrays.asList(AgentStatus.AVAILABLE, AgentStatus.BUSY)
        );

        if (availableAgents.isEmpty()) {
            log.error("No online agents available for emergency auto-reassignment fallback.");
            return;
        }

        // Get the offline agent entity to decrement workload counters
        Agent offlineAgent = agentRepository.findById(event.getAgentId()).orElse(null);

        for (Order order : impactedOrders) {
            ReassignmentSuggestion suggestion;
            RoutingStrategy activeStrategy = engineContext.getActiveStrategy();

            // 3. Evaluate the recommendation strategy target[cite: 27]
            try {
                suggestion = activeStrategy.suggestReassignment(order, availableAgents, TriggerReason.AGENT_OFFLINE);
            } catch (Exception ex) {
                log.error("AI strategy failed during auto-replan. Falling back to Rule-Based.", ex);
                suggestion = ruleBasedFallback.suggestReassignment(order, availableAgents, TriggerReason.AGENT_OFFLINE);
            }

            // 4. AUTOMATIC ASSIGNMENT LAYER (Replaces standard manual review checks)
            String targetAgentId = suggestion.getRecommendedAgentId();
            
            if (targetAgentId != null) {
                Agent targetAgent = agentRepository.findById(targetAgentId).orElse(null);
                
                if (targetAgent != null) {
                    // Update the workload metrics for the new agent
                    targetAgent.setActiveOrderCount(targetAgent.getActiveOrderCount() + 1);
                    agentRepository.save(targetAgent);

                    // Decrement the workload counter for the old offline agent
                    if (offlineAgent != null) {
                        offlineAgent.setActiveOrderCount(Math.max(0, offlineAgent.getActiveOrderCount() - 1));
                    }

                    // Directly shift order assignment values on the database layer
                    order.setAssignedAgentId(targetAgentId);
                    order.setStatus(OrderStatus.REASSIGNED);
                    orderRepository.save(order);

                    // Mark suggestion as automatically completed for record history logs
                    suggestion.setStatus(SuggestionStatus.ACCEPTED);
                    suggestion.setReasoning("[Auto-Assigned]: " + suggestion.getReasoning());
                    suggestionRepository.save(suggestion);
                    
                    log.info("Order {} automatically reassigned to Agent {} successfully.", order.getId(), targetAgentId);
                }
            }
        }
        
        if (offlineAgent != null) {
            agentRepository.save(offlineAgent);
        }
    }
}