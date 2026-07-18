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

    public List<Order> getOrders(OrderStatus status) {
        return (status != null) ? orderRepository.findByStatus(status) : orderRepository.findAll();
    }
}
