package com.qsp.resolvehub.dto.response;

public record AgentWorkloadResponse(
        Integer agentId,
        String agentName,
        String agentEmail,
        long assignedTickets
) {
}