package com.test.hackathon_backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {
    @Id
    private String id;
    private String name;
    private int activeOrderCount;
    
    @Enumerated(EnumType.STRING)
    private AgentStatus status;
    
    // Sprint 2 - Extension
    // private String homeZoneId;
    // private Double maxWeightCapacity;
}