package com.test.hackathon_backend.service;

import com.test.hackathon_backend.domain.Order;
import com.test.hackathon_backend.domain.OrderStatus;
import com.test.hackathon_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    public Order createOrder(Order order) {
        order.setStatus(OrderStatus.ASSIGNED);
        order.setCreatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public List<Order> getOrders(String id, OrderStatus status, String agentId) {
        // If an ID is passed, we likely just want that specific order packaged in a list
        if (id != null) {
            return orderRepository.findById(id)
                    .map(List::of)
                    .orElse(List.of());
        }

        
        // Filter by both status and agentId
        if (status != null && agentId != null) {
            return orderRepository.findByAssignedAgentIdAndStatusNot(agentId, status);
        } 
        // Filter by status only
        else if (status != null) {
            return orderRepository.findByStatus(status);
        } 
        // Filter by agentId only
        else if (agentId != null) {
            return orderRepository.findByAssignedAgentId(agentId);
        }
        
        // Fallback: return everything if no filters are provided
        return orderRepository.findAll();
    }
    // Add to the bottom of OrderService class:
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
    }
}
