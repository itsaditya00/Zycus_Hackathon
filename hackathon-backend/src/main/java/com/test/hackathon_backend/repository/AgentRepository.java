package com.test.hackathon_backend.repository;

import com.test.hackathon_backend.domain.Agent;
import com.test.hackathon_backend.domain.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, String> {
    List<Agent> findByStatusIn(List<AgentStatus> statuses);

    @Query("SELECT a FROM Agent a WHERE a.status = :status")
    List<Agent> findByStatus(@Param("status") AgentStatus status);
}
