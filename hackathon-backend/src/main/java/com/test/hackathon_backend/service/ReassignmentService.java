package com.test.hackathon_backend.service;

import com.test.hackathon_backend.domain.*;
import com.test.hackathon_backend.repository.*;
import com.test.hackathon_backend.strategy.RoutingEngineContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReassignmentService {
    private final ReassignmentSuggestionRepository suggestionRepository;
    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final RoutingEngineContext engineContext;

    @Transactional
    public ReassignmentSuggestion processDecision(String orderId, SuggestionStatus targetDecision) {
        // 1. Find the active pending suggestion for this specific order
        ReassignmentSuggestion suggestion = suggestionRepository
            .findByOrderIdAndStatus(orderId, SuggestionStatus.PENDING)
            .orElseThrow(() -> new IllegalStateException("No pending reassignment suggestion found for this order."));

        // 2. Apply the target decision
        suggestion.setStatus(targetDecision);
        
        if (targetDecision == SuggestionStatus.ACCEPTED) {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Associated order resource missing."));
            
            // Decrement workload counter for the previous agent
            if (order.getAssignedAgentId() != null) {
                agentRepository.findById(order.getAssignedAgentId()).ifPresent(pastAgent -> {
                    pastAgent.setActiveOrderCount(Math.max(0, pastAgent.getActiveOrderCount() - 1));
                    // NOTE: agentRepository.save(pastAgent) is optional due to @Transactional dirty checking, but fine to keep
                    agentRepository.save(pastAgent);
                });
            }

            // Increment workload counter for the new agent
            Agent newAgent = agentRepository.findById(suggestion.getRecommendedAgentId())
                .orElseThrow(() -> new IllegalStateException("Recommended target agent missing."));
            
            newAgent.setActiveOrderCount(newAgent.getActiveOrderCount() + 1);
            agentRepository.save(newAgent);

            // Update order assignment details
            order.setAssignedAgentId(newAgent.getId());
            order.setStatus(OrderStatus.REASSIGNED);
            orderRepository.save(order);

        } else if (targetDecision == SuggestionStatus.REJECTED) {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Associated order resource missing."));
            
            order.setStatus(OrderStatus.ASSIGNED);
            orderRepository.save(order);
        }

        return suggestionRepository.save(suggestion);
    }

    @Transactional
    public ReassignmentSuggestion createOnDemandSuggestion(String id, String strategy) {
        // 1. Fetch the target order
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

        // 2. Fetch all available agents who can take the order
        List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);

        if (availableAgents.isEmpty()) {
            throw new IllegalStateException("No available agents found for reassignment.");
        }

        // 3. UPDATED: Call the strategy registry using the context override workflow
        ReassignmentSuggestion suggestion = engineContext.executeStrategyWithOverride(
                order, 
                availableAgents, 
                TriggerReason.INITIAL, 
                strategy
        );

        // 4. Persist and return the suggestion
        return suggestionRepository.save(suggestion);
    }
}