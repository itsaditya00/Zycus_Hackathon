package com.test.hackathon_backend.strategy;

import com.test.hackathon_backend.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.List;
// import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component("AI_POWERED")
public class AIRoutingStrategy implements RoutingStrategy {

    private final LlmGateway llmGateway;
    private final RoutingStrategy fallbackStrategy;

    public AIRoutingStrategy(LlmGateway llmGateway, @Qualifier("RULE_BASED") RoutingStrategy fallbackStrategy) {
        this.llmGateway = llmGateway;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public ReassignmentSuggestion suggestReassignment(Order order, List<Agent> availableAgents, TriggerReason reason) {
        // 1. Keep AI calls off the critical request path by spinning up a non-blocking background task
        String prompt = constructContextualPrompt(order, availableAgents, reason);
        
        // Generate an immediate placeholder using rule-based metrics to ensure zero system downtime
        ReassignmentSuggestion quickSuggestion = fallbackStrategy.suggestReassignment(order, availableAgents, reason);
        
        // 2. Fork execution to complete the AI optimization asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Initiating asynchronous LLM optimization routing analysis via gemini-3.1-flash-lite...");
                LlmGateway.LlmResponse response = llmGateway.callLLM(prompt);

                // 3. Cross-validate that the LLM did not hallucinate an agent identifier
                boolean agentExists = availableAgents.stream()
                        .anyMatch(a -> a.getId().equals(response.getRecommendedAgentId()));

                if (!agentExists) {
                    log.warn("AI hallucinated non-existent agent ID: {}. Retaining rule-based assignment fallback.", 
                            response.getRecommendedAgentId());
                    return;
                }

                // 4. Update the suggestion details dynamically with AI optimization insights
                quickSuggestion.setRecommendedAgentId(response.getRecommendedAgentId());
                quickSuggestion.setConfidenceScore(response.getConfidenceScore());
                // Reason text is placed directly into the suggestion object to be displayed verbatim in the UI
                quickSuggestion.setReasoning("AI Optimized: " + response.getReasoning());
                quickSuggestion.setStatus(SuggestionStatus.ACCEPTED);
                
                log.info("Successfully optimized route via AI for Order ID: {} assigned to Agent: {}", 
                        order.getId(), response.getRecommendedAgentId());

            } catch (Exception ex) {
                log.error("AI routing optimization runtime error. Gracefully falling back to rule-based suggestion context.", ex);
            }
        });

        return quickSuggestion;
    }

    private String constructContextualPrompt(Order order, List<Agent> agents, TriggerReason reason) {
        String baseRoster = agents.stream()
            .map(a -> String.format("- Agent ID: '%s', Name: %s, Current Load: %d orders", a.getId(), a.getName(), a.getActiveOrderCount()))
            .collect(Collectors.joining("\n"));

        // Distinct prompts providing specialized contextual signals depending on the trigger type
        if (reason == TriggerReason.AGENT_OFFLINE) {
            // Count how many orders were potentially affected by looking at current available capacity/agents if applicable
            long strandedOrdersEstimate = 1; // Minimum baseline is the current order being re-routed
            
            return String.format(
                "### CRITICAL OPERATIONAL RECOVERY PROMPT ###\n" +
                "CONTEXT: Emergency Recovery routing needed! An active agent went OFFLINE mid-shift unexpectedly.\n" +
                "Stranded Order Context:\n" +
                "- Order ID: %s\n" +
                "- Order Description: %s\n" +
                "- Failing/Previous Offline Agent ID: %s\n" +
                "- Context Alert: At least %d order(s) are currently stranded and require immediate stabilization.\n\n" +
                "Available Agent Candidates Roster:\n%s\n\n" +
                "TASK: Balance load profile carefully to avoid overwhelming remaining agents. Return valid, plain raw JSON matching exactly:\n" +
                "{\n" +
                "  \"recommendedAgentId\": \"string\",\n" +
                "  \"confidenceScore\": double (0.0 to 1.0),\n" +
                "  \"reasoning\": \"string plain English operational reason explanation for UI display\"\n" +
                "}",
                order.getId(), order.getDescription(), order.getAssignedAgentId(), strandedOrdersEstimate, baseRoster
            );
        } else {
            return String.format(
                "### INITIAL DAY-SETUP ROUTING PROMPT ###\n" +
                "CONTEXT: Standard proactive routing configuration setup.\n" +
                "Target Order Details:\n" +
                "- Order ID: %s\n" +
                "- Order Description: %s\n\n" +
                "Available Agent Candidates Roster:\n%s\n\n" +
                "TASK: Distribute tasks evenly to maximize early-shift dispatch efficiency. Return valid, plain raw JSON matching exactly:\n" +
                "{\n" +
                "  \"recommendedAgentId\": \"string\",\n" +
                "  \"confidenceScore\": double (0.0 to 1.0),\n" +
                "  \"reasoning\": \"string plain English operational reason explanation for UI display\"\n" +
                "}",
                order.getId(), order.getDescription(), baseRoster
            );
        }
    }

    @Override
    public String getStrategyName() {
        return "AI_POWERED";
    }
}


















// package com.test.hackathon_backend.strategy;


// import com.test.hackathon_backend.domain.*;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.stereotype.Component;
// import java.util.List;
// import java.util.UUID;
// import java.util.stream.Collectors;

// @Slf4j
// @Component("AI_POWERED")
// public class AIRoutingStrategy implements RoutingStrategy {

//     private final LlmGateway llmGateway;
//     private final RoutingStrategy fallbackStrategy;

//     public AIRoutingStrategy(LlmGateway llmGateway, @Qualifier("RULE_BASED") RoutingStrategy fallbackStrategy) {
//         this.llmGateway = llmGateway;
//         this.fallbackStrategy = fallbackStrategy;
//     }

//     @Override
//     public ReassignmentSuggestion suggestReassignment(Order order, List<Agent> availableAgents, TriggerReason reason) {
//         try {
//             String prompt = constructContextualPrompt(order, availableAgents, reason);
//             LlmGateway.LlmResponse response = llmGateway.callLLM(prompt);

//             // Cross-validate that the LLM did not hallucinate an agent identifier
//             boolean agentExists = availableAgents.stream().anyMatch(a -> a.getId().equals(response.getRecommendedAgentId()));
//             if (!agentExists) {
//                 log.warn("AI hallucinated agent ID: {}. Triggering rule-based fallback.", response.getRecommendedAgentId());
//                 return fallbackStrategy.suggestReassignment(order, availableAgents, reason);
//             }

//             return ReassignmentSuggestion.builder()
//                 .id(UUID.randomUUID().toString())
//                 .orderId(order.getId())
//                 .recommendedAgentId(response.getRecommendedAgentId())
//                 .confidenceScore(response.getConfidenceScore())
//                 .reasoning(response.getReasoning())
//                 .status(SuggestionStatus.PENDING)
//                 .triggerReason(reason)
//                 .build();

//         } catch (Exception ex) {
//             log.error("AI strategy encountered an execution failure. Falling back seamlessly.", ex);
//             return fallbackStrategy.suggestReassignment(order, availableAgents, reason);
//         }
//     }

//     private String constructContextualPrompt(Order order, List<Agent> agents, TriggerReason reason) {
//         String baseRoster = agents.stream()
//             .map(a -> String.format("- Agent ID: %s, Name: %s, Active Loads: %d", a.getId(), a.getName(), a.getActiveOrderCount()))
//             .collect(Collectors.joining("\n"));

//         if (reason == TriggerReason.AGENT_OFFLINE) {
//             return String.format(
//                 "CRITICAL RECOVERY CONTEXT: An agent went OFFLINE unexpectedly mid-shift.\n" +
//                 "Stranded Order: [ID: %s, Description: %s, Past Agent: %s]\n" +
//                 "Available Candidates Roster:\n%s\n" +
//                 "Provide optimization assignment choice. Return raw JSON matching fields strictly: recommendedAgentId, confidenceScore (0.0-1.0), reasoning.",
//                 order.getId(), order.getDescription(), order.getAssignedAgentId(), baseRoster
//             );
//         } else {
//             return String.format(
//                 "ROUTING CONTEXT: Initial Day Setup Routing Request.\n" +
//                 "Target Order: [ID: %s, Description: %s]\n" +
//                 "Available Candidates Roster:\n%s\n" +
//                 "Provide optimal assignment choice. Return raw JSON matching fields strictly: recommendedAgentId, confidenceScore (0.0-1.0), reasoning.",
//                 order.getId(), order.getDescription(), baseRoster
//             );
//         }
//     }

//     @Override
//     public String getStrategyName() {
//         return "AI_POWERED";
//     }
// }