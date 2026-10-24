package com.qsp.resolvehub.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignTicketRequest(

        @NotNull(message = "Agent ID is required")
        @Positive(message = "Agent ID must be positive")
        Integer agentId

) {
}