package com.test.hackathon_backend.repository;

import com.test.hackathon_backend.domain.ReassignmentSuggestion;
import com.test.hackathon_backend.domain.SuggestionStatus;
import com.test.hackathon_backend.domain.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReassignmentSuggestionRepository extends JpaRepository<ReassignmentSuggestion, String> {
    Optional<ReassignmentSuggestion> findByOrderIdAndStatusAndTriggerReason(
        String orderId, SuggestionStatus status, TriggerReason reason
    );
}