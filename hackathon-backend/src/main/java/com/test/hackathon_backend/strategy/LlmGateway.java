package com.test.hackathon_backend.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LlmGateway {

    @Value("${llm.provider}")
    private String provider;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    @Value("${llm.base-url}")
    private String baseUrl;

    private final RestClient http = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class LlmResponse {
        private String recommendedAgentId;
        private double confidenceScore;
        private String reasoning;
    }

    /**
     * Sends prompt to the configured LLM and returns the structured LlmResponse.
     */
    public LlmResponse callLLM(String prompt) {
        String rawResponse = switch (provider.toLowerCase()) {
            case "gemini" -> callGemini(prompt);
            case "groq"   -> callOpenAICompatible(prompt, baseUrl + "/openai/v1/chat/completions");
            case "ollama" -> callOpenAICompatible(prompt, baseUrl + "/v1/chat/completions");
            default       -> throw new IllegalStateException("Unknown provider: " + provider);
        };

        try {
            // Parse the raw JSON string out into our structured response class
            return objectMapper.readValue(rawResponse, LlmResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response into structured JSON object: " + rawResponse, e);
        }
    }

    private String callGemini(String prompt) {
        var url  = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        var body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        var resp = http.post().uri(url)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(body).retrieve().body(Map.class);

        try {
            var candidates = (List<?>) resp.get("candidates");
            var content    = (Map<?,?>)((Map<?,?>) candidates.get(0)).get("content");
            var parts      = (List<?>) content.get("parts");
            return (String)((Map<?,?>) parts.get(0)).get("text");
        } catch (Exception e) {
            throw new RuntimeException("Gemini response parse failed", e);
        }
    }

    private String callOpenAICompatible(String prompt, String url) {
        var body = Map.of(
            "model",    model,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        var resp = http.post().uri(url)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + apiKey)
            .body(body).retrieve().body(Map.class);

        try {
            var choices = (List<?>) resp.get("choices");
            var message = (Map<?,?>)((Map<?,?>) choices.get(0)).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("LLM response parse failed", e);
        }
    }
}




// package com.test.hackathon_backend.strategy;

// import lombok.Data;
// import org.springframework.stereotype.Component;

// @Component
// public class LlmGateway {

//     @Data
//     public static class LlmResponse {
//         private String recommendedAgentId;
//         private double confidenceScore;
//         private String reasoning;
//     }

//     public LlmResponse callLLM(String prompt) {
//         // Boilerplate stub simulating external API network call with structural response parsing
//         LlmResponse response = new LlmResponse();
//         response.setRecommendedAgentId("AGT-002"); // Resolves dynamically against test configuration metrics
//         response.setConfidenceScore(0.92);
//         response.setReasoning("Selected via analytical distribution profiling: Agent AGT-002 currently exhibits zero system load profiles.");
//         return response;
//     }
// }
