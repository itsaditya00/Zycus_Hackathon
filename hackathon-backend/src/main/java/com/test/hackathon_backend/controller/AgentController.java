package com.test.hackathon_backend.controller;

import com.test.hackathon_backend.domain.Agent;
import com.test.hackathon_backend.domain.AgentStatus;
import com.test.hackathon_backend.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {
    private final AgentService agentService;

    @PatchMapping("/{id}/status")
    public ResponseEntity<Agent> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        AgentStatus status = AgentStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(agentService.updateAgentStatus(id, status));
    }
}
