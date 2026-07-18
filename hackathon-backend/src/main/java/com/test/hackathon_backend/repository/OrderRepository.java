package com.test.hackathon_backend.repository;

import com.test.hackathon_backend.domain.Order;
import com.test.hackathon_backend.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByAssignedAgentIdAndStatusNot(String agentId, OrderStatus status);
}