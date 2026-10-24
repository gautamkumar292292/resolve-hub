package com.qsp.resolvehub.dto.request;

import com.qsp.resolvehub.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(

        @NotNull(message = "Ticket status is required")
        TicketStatus status

) {
}