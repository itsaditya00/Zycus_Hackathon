package com.test.hackathon_backend.event;

import com.test.hackathon_backend.domain.*;
import com.test.hackathon_backend.repository.*;
import com.test.hackathon_backend.strategy.RoutingEngineContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgenticReplanListener {

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final ReassignmentSuggestionRepository suggestionRepository;
    private final RoutingEngineContext engineContext;

    @Async
    @EventListener
    public void handleAgentOffline(AgentOfflineEvent event) {
        log.info("Async Agentic Re-planning sequence initiated for Agent: {}", event.getAgentId());
        
        // Find all active orders assigned to the offline agent
        List<Order> impactedOrders = orderRepository.findByAssignedAgentIdAndStatusNot(
            event.getAgentId(), OrderStatus.DELIVERED
        );

        if (impactedOrders.isEmpty()) {
            log.info("No active orders impacted by agent {} going offline.", event.getAgentId());
            return;
        }

        // Fetch eligible backup candidates
        List<Agent> availableAgents = agentRepository.findByStatusIn(
            Arrays.asList(AgentStatus.AVAILABLE, AgentStatus.BUSY)
        );

        if (availableAgents.isEmpty()) {
            log.error("Critical: No operational fallback routing targets found to allocate recovery objects.");
            return;
        }

        for (Order order : impactedOrders) {
            // Deduplication Check
            boolean exists = suggestionRepository.findByOrderIdAndStatusAndTriggerReason(
                order.getId(), SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE
            ).isPresent();

            if (exists) {
                log.info("Pending suggestion already registered for order: {}. Skipping.", order.getId());
                continue;
            }

            // Flag order state internally
            order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
            orderRepository.save(order);

            // Execute processing strategy via configuration context
            ReassignmentSuggestion suggestion = engineContext.getActiveStrategy()
                .suggestReassignment(order, availableAgents, TriggerReason.AGENT_OFFLINE);
            
            suggestionRepository.save(suggestion);
            log.info("Persisted new recovery recommendation entity {} for order {}", suggestion.getId(), order.getId());
        }
    }
}
