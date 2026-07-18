// File: com/test/hackathon_backend/strategy/AIRoutingStrategy.java
package com.test.hackathon_backend.strategy;

import com.test.hackathon_backend.domain.*;
import com.test.hackathon_backend.repository.ReassignmentSuggestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

@Slf4j
@Component("AI_POWERED")
public class AIRoutingStrategy implements RoutingStrategy {

    private final LlmGateway llmGateway;
    private final RoutingStrategy fallbackStrategy;
    private final ReassignmentSuggestionRepository suggestionRepository; // Injected for real persistence

    public AIRoutingStrategy(LlmGateway llmGateway, 
                              @Qualifier("RULE_BASED") RoutingStrategy fallbackStrategy,
                              ReassignmentSuggestionRepository suggestionRepository) {
        this.llmGateway = llmGateway;
        this.fallbackStrategy = fallbackStrategy;
        this.suggestionRepository = suggestionRepository;
    }

    @Override
    public ReassignmentSuggestion suggestReassignment(Order order, List<Agent> availableAgents, TriggerReason reason) {
        // 1. Check if an active PENDING suggestion already exists for this order to prevent duplicates
        java.util.Optional<ReassignmentSuggestion> existingSuggestion = 
            suggestionRepository.findByOrderIdAndStatus(order.getId(), SuggestionStatus.PENDING);

        final ReassignmentSuggestion targetSuggestion;

        if (existingSuggestion.isPresent()) {
            // Reuse the existing record instead of creating a new row
            targetSuggestion = existingSuggestion.get();
            targetSuggestion.setReasoning("AI re-optimization processing initiated... Standby.");
            targetSuggestion.setTriggerReason(reason); 
        } else {
            // Compute baseline fallback placeholder only if no active suggestion exists
            targetSuggestion = fallbackStrategy.suggestReassignment(order, availableAgents, reason);
            targetSuggestion.setReasoning("AI optimization processing initiated... Standby.");
        }
        
        // Save to update or insert, establishing/retaining the database identity
        final ReassignmentSuggestion savedSuggestion = suggestionRepository.save(targetSuggestion);
        final String targetSuggestionId = savedSuggestion.getId();

        // Generate the contextual prompt before launching the async thread
        final String prompt = constructContextualPrompt(order, availableAgents, reason);

        // 2. Offload high-latency AI network tasks to asynchronous worker pool
        CompletableFuture.runAsync(() -> {
            try {
                LlmGateway.LlmResponse response = llmGateway.callLLM(prompt);

                // Validation against hallucinated identifiers
                boolean agentExists = availableAgents.stream().anyMatch(a -> a.getId().equals(response.getRecommendedAgentId()));
                
                ReassignmentSuggestion activeSuggestion = suggestionRepository.findById(targetSuggestionId)
                        .orElse(savedSuggestion);

                if (!agentExists) {
                    log.warn("AI hallucinated agent ID: {}. Retaining fallback.", response.getRecommendedAgentId());
                    activeSuggestion.setReasoning("AI optimization failed (Hallucination Guard Intercepted). " + activeSuggestion.getReasoning());
                    suggestionRepository.save(activeSuggestion);
                    return;
                }

                // Store plain-English reasoning verbatim in the suggestion for Ops
                activeSuggestion.setRecommendedAgentId(response.getRecommendedAgentId());
                activeSuggestion.setConfidenceScore(response.getConfidenceScore());
                activeSuggestion.setReasoning(response.getReasoning());
                activeSuggestion.setStatus(SuggestionStatus.PENDING);
                
                // Commit directly back to the database so UI polls capture it
                suggestionRepository.save(activeSuggestion);
            } catch (Exception ex) {
                log.error("Async AI computation failed. Retaining rule baseline.", ex);
            }
        });

        return savedSuggestion;
    }

    private String constructContextualPrompt(Order order, List<Agent> agents, TriggerReason reason) {
        String baseRoster = agents.stream()
            .map(a -> String.format("- Agent ID: '%s', Name: %s, Workload: %d active orders", a.getId(), a.getName(), a.getActiveOrderCount()))
            .collect(Collectors.joining("\n"));

        // Scenario A: Critical Mid-Shift Operational Recovery Action
        if (reason == TriggerReason.AGENT_OFFLINE) {
            return String.format(
                "### CRITICAL OPERATIONAL RECOVERY PROMPT ###\n" +
                "CONTEXT: Emergency recovery routine! Agent '%s' went offline unexpectedly.\n" +
                "Stranded Order Details:\n" +
                "- Order ID: %s\n" +
                "- Description: %s\n" +
                "System Roster State:\n%s\n\n" +
                "TASK: Balance load profile to prevent bottleneck cascading. Respond ONLY with raw, valid JSON:\n" +
                "{\n  \"recommendedAgentId\": \"string\",\n  \"confidenceScore\": double,\n  \"reasoning\": \"string\"\n}",
                order.getAssignedAgentId(), order.getId(), order.getDescription(), baseRoster
            );
        } 
        // Scenario B: Standard Proactive Capacity Shift Initial Mapping Allocation
        else {
            return String.format(
                "### INITIAL SETUP ROUTING PROMPT ###\n" +
                "CONTEXT: Proactive Day-Setup distribution routine allocation optimization.\n" +
                "Target Order Details:\n" +
                "- Order ID: %s\n" +
                "- Description: %s\n" +
                "System Roster State:\n%s\n\n" +
                "TASK: Maximize processing velocity across teams. Respond ONLY with raw, valid JSON:\n" +
                "{\n  \"recommendedAgentId\": \"string\",\n  \"confidenceScore\": double,\n  \"reasoning\": \"string\"\n}",
                order.getId(), order.getDescription(), baseRoster
            );
        }
    }

    @Override
    public String getStrategyName() {
        return "AI_POWERED";
    }

    @Override
    public void streamReassignmentReasoning(Order order, List<Agent> availableAgents, TriggerReason reason, SseEmitter emitter) {
        try {
            // 1. Send an initial status update to the client
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data("Analyzing order details and routing constraints..."));

            // 2. Construct the prompt using your existing method
            String prompt = constructContextualPrompt(order, availableAgents, reason);
            
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data("Consulting AI model for optimal routing strategy..."));

            // 3. Call the LLM (Since OrderController runs this asynchronously, a blocking call here is fine)
            LlmGateway.LlmResponse response = llmGateway.callLLM(prompt);

            // 4. Validate the response
            boolean agentExists = availableAgents.stream()
                    .anyMatch(a -> a.getId().equals(response.getRecommendedAgentId()));

            if (!agentExists) {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data("AI suggested an invalid agent. Reverting to fallback strategy."));
                
                emitter.complete();
                return;
            }

            // 5. Stream the final successful reasoning back to the UI
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data("Optimal Agent Found: " + response.getRecommendedAgentId()));

            emitter.send(SseEmitter.event()
                    .name("reasoning")
                    .data(response.getReasoning()));

            // 6. Complete the stream cleanly
            emitter.complete();

        } catch (IOException e) {
            log.error("Failed to send SSE event", e);
            emitter.completeWithError(e);
        } catch (Exception e) {
            log.error("AI Streaming computation failed", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("AI engine failed to produce reasoning: " + e.getMessage()));
            } catch (IOException ioException) {
                log.error("Failed to send error event to emitter", ioException);
            }
            emitter.completeWithError(e);
        }
    }
}