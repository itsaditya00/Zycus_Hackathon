package com.test.hackathon_backend.controller;

import com.test.hackathon_backend.domain.Agent;
import com.test.hackathon_backend.domain.AgentStatus;
import com.test.hackathon_backend.domain.Order;
import com.test.hackathon_backend.domain.OrderStatus;
import com.test.hackathon_backend.service.AgentService;
import com.test.hackathon_backend.service.OrderService;
import com.test.hackathon_backend.domain.ReassignmentSuggestion;
import com.test.hackathon_backend.domain.TriggerReason;
import com.test.hackathon_backend.service.ReassignmentService;
import com.test.hackathon_backend.strategy.RoutingEngineContext;
import com.test.hackathon_backend.strategy.RoutingStrategy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {
    private final OrderService orderService;
    private final ReassignmentService reassignmentService;
    private final AgentService agentService;
    private final RoutingEngineContext engineContext;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String agentId) {
        return ResponseEntity.ok(orderService.getOrders(id, status, agentId));
    }

    @PostMapping("/{id}/suggest")
    public ResponseEntity<ReassignmentSuggestion> generateSuggestion(
            @PathVariable String id, 
            @RequestParam(required = false) String strategy) {
        
        // If strategy is null/blank, the service layer defaults it to AI
        ReassignmentSuggestion suggestion = reassignmentService.createOnDemandSuggestion(id, strategy);
        return ResponseEntity.ok(suggestion);
    }

    // File: Add endpoint to com/test/hackathon_backend/controller/OrderController.java

    @GetMapping(value = "/{id}/suggest/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSuggestionReasoning(
            @PathVariable String id, 
            @RequestParam(required = false) String strategy) {
        
        SseEmitter emitter = new SseEmitter(30000L);
        
        Order order = orderService.getOrderById(id); 
        // Uses the new query method we're adding to AgentService below
        List<Agent> availableAgents = agentService.getAvailableAgents(AgentStatus.AVAILABLE);
        
        // Uses the context routing override method you added
        RoutingStrategy routingStrategy = engineContext.resolveStrategy(strategy);
        
        CompletableFuture.runAsync(() -> {
            routingStrategy.streamReassignmentReasoning(order, availableAgents, TriggerReason.INITIAL, emitter);
        });
        
        return emitter;
    }
}
