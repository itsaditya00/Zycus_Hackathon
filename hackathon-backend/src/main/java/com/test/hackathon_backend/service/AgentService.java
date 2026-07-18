package com.test.hackathon_backend.service;

import com.test.hackathon_backend.domain.Agent;
import com.test.hackathon_backend.domain.AgentStatus;
import com.test.hackathon_backend.event.AgentOfflineEvent;
import com.test.hackathon_backend.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentService {
    private final AgentRepository agentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Agent updateAgentStatus(String id, AgentStatus newStatus) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Agent resource target not found."));
        
        AgentStatus oldStatus = agent.getStatus();
        agent.setStatus(newStatus);
        Agent updatedAgent = agentRepository.save(agent);

        if (newStatus == AgentStatus.OFFLINE && oldStatus != AgentStatus.OFFLINE) {
            // Decouple through structural app event injection context boundaries
            eventPublisher.publishEvent(new AgentOfflineEvent(this, id));
        }

        return updatedAgent;
    }
}