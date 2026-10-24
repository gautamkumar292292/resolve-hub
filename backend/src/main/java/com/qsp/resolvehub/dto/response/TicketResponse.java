
package com.qsp.resolvehub.dto.response;

import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.TicketPriority;

import java.time.LocalDateTime;

public record TicketResponse(
        Integer id,
        String ticketNumber,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String requesterName,
        String assignedAgentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getRequester() != null
                        ? ticket.getRequester().getName() : null,
                ticket.getAssignedTo() != null
                        ? ticket.getAssignedTo().getName() : null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt()
        );
    }
}
