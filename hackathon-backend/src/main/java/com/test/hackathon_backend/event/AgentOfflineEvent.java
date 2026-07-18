package com.test.hackathon_backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AgentOfflineEvent extends ApplicationEvent {
    private final String agentId;

    public AgentOfflineEvent(Object source, String agentId) {
        super(source);
        this.agentId = agentId;
    }
}