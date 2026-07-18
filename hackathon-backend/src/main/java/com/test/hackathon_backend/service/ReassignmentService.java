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
    public ReassignmentSuggestion processDecision(String suggestionId, SuggestionStatus targetDecision) {
        ReassignmentSuggestion suggestion = suggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new IllegalArgumentException("Target suggestion entity not found."));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion evaluation already determined.");
        }

        suggestion.setStatus(targetDecision);
        
        if (targetDecision == SuggestionStatus.ACCEPTED) {
            Order order = orderRepository.findById(suggestion.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Associated order resource missing."));
            
            // Re-allocate workload counters across tracking entities
            if (order.getAssignedAgentId() != null) {
                agentRepository.findById(order.getAssignedAgentId()).ifPresent(pastAgent -> {
                    pastAgent.setActiveOrderCount(Math.max(0, pastAgent.getActiveOrderCount() - 1));
                    agentRepository.save(pastAgent);
                });
            }

            Agent newAgent = agentRepository.findById(suggestion.getRecommendedAgentId())
                .orElseThrow(() -> new IllegalStateException("Recommended target agent missing."));
            
            newAgent.setActiveOrderCount(newAgent.getActiveOrderCount() + 1);
            agentRepository.save(newAgent);

            // Commit reassignment resolution payload values
            order.setAssignedAgentId(newAgent.getId());
            order.setStatus(OrderStatus.REASSIGNED);
            orderRepository.save(order);
        } else if (targetDecision == SuggestionStatus.REJECTED) {
            orderRepository.findById(suggestion.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.ASSIGNED);
                orderRepository.save(order);
            });
        }

        return suggestionRepository.save(suggestion);
    }

    @Transactional
    public ReassignmentSuggestion createOnDemandSuggestion(String id) {
        // 1. Fetch the target order
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

        // 2. Fetch all available agents who can take the order
        List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);

        if (availableAgents.isEmpty()) {
            throw new IllegalStateException("No available agents found for reassignment.");
        }

        // 3. Directly execute the active strategy which builds the full ReassignmentSuggestion payload
        // We pass order, availableAgents, and the TriggerReason to match the context's signature
        ReassignmentSuggestion suggestion = engineContext.executeStrategy(order, availableAgents, TriggerReason.INITIAL);

        // 4. Persist and return the suggestion
        return suggestionRepository.save(suggestion);
    }

    // @Transactional
    // public ReassignmentSuggestion createOnDemandSuggestion(String id) {
    //     // 1. Fetch the target order
    //     Order order = orderRepository.findById(id)
    //         .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

    //     // 2. Fetch all available agents who can take the order (e.g., status is AVAILABLE)
    //     // Adjust the query method name based on your AgentRepository definitions
    //     List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);

    //     if (availableAgents.isEmpty()) {
    //         throw new IllegalStateException("No available agents found for reassignment.");
    //     }

    //     // 3. Delegate to the pluggable routing engine context to execute the active strategy
    //     // The engine returns an ordered list of recommendations; pick the best one (index 0)
    //     List<Agent> recommendations = engineContext.executeStrategy(order, availableAgents);
    //     if (recommendations == null || recommendations.isEmpty()) {
    //         throw new IllegalStateException("Routing strategy could not provide any agent recommendations.");
    //     }
    //     Agent recommendedAgent = recommendations.get(0);

    //     // 4. Construct the ReassignmentSuggestion entity
    //     ReassignmentSuggestion suggestion = new ReassignmentSuggestion();
    //     suggestion.setOrderId(order.getId());
    //     suggestion.setRecommendedAgentId(recommendedAgent.getId());
    //     suggestion.setStatus(SuggestionStatus.PENDING);
        
    //     // If your domain model tracks why it was triggered (e.g., ON_DEMAND)
    //     suggestion.setTriggerReason(TriggerReason.INITIAL); 

    //     // 5. Persist and return the suggestion
    //     return suggestionRepository.save(suggestion);
    // }
}