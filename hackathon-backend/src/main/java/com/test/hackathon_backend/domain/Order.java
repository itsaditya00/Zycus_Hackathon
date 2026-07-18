package com.test.hackathon_backend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    private String id;
    private String description;
    private String assignedAgentId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private LocalDateTime createdAt;
    
    // Sprint 2 - Extension Placeholders
    // private String zoneId;
    // private Double weightKg;
    // private LocalDateTime slaDeadline;
}