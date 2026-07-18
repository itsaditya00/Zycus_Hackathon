package com.test.hackathon_backend.controller;

import com.test.hackathon_backend.domain.ReassignmentSuggestion;
import com.test.hackathon_backend.domain.SuggestionStatus;
import com.test.hackathon_backend.service.ReassignmentService;
import com.test.hackathon_backend.strategy.RoutingEngineContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
public class ReassignmentSuggestionController {
    private final ReassignmentService reassignmentService;
    private final RoutingEngineContext engineContext;

    @PatchMapping("/{id}")
    public ResponseEntity<ReassignmentSuggestion> patchSuggestion(
            @PathVariable String id, 
            @RequestBody Map<String, String> body) {
        SuggestionStatus decision = SuggestionStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(reassignmentService.processDecision(id, decision));
    }

    @PutMapping("/strategy")
    public ResponseEntity<Map<String, String>> switchStrategy(@RequestBody Map<String, String> body) {
        String targetStrategy = body.get("strategy");
        engineContext.setActiveStrategy(targetStrategy);
        return ResponseEntity.ok(Map.of("activeStrategy", targetStrategy));
    }
}
