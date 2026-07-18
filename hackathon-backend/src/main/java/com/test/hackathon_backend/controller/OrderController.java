package com.test.hackathon_backend.controller;

import com.test.hackathon_backend.domain.Order;
import com.test.hackathon_backend.domain.OrderStatus;
import com.test.hackathon_backend.service.OrderService;
import com.test.hackathon_backend.domain.ReassignmentSuggestion;
import com.test.hackathon_backend.service.ReassignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final ReassignmentService reassignmentService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrders(status));
    }

    @PostMapping("/{id}/suggest")
    public ResponseEntity<ReassignmentSuggestion> generateSuggestion(@PathVariable String id) {
        // Run the active strategy, persist the suggestion, and return it
        ReassignmentSuggestion suggestion = reassignmentService.createOnDemandSuggestion(id);
        return ResponseEntity.ok(suggestion);
    }
}
