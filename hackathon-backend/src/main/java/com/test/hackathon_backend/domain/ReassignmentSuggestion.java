package com.test.hackathon_backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reassignment_suggestions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReassignmentSuggestion {
    @Id
    private String id;
    private String orderId;
    private String recommendedAgentId;
    private double confidenceScore;
    
    @Column(length = 1024)
    private String reasoning;
    
    @Enumerated(EnumType.STRING)
    private SuggestionStatus status;
    
    @Enumerated(EnumType.STRING)
    private TriggerReason triggerReason;
}
