
package com.qsp.resolvehub.dto.response;

import com.qsp.resolvehub.entity.TicketHistory;
import com.qsp.resolvehub.entity.TicketStatus;

import java.time.LocalDateTime;

public record TicketHistoryResponse(
        Integer id,
        Integer ticketId,
        String changedByEmail,
        TicketStatus oldStatus,
        TicketStatus newStatus,
        LocalDateTime changedAt
) {

    public static TicketHistoryResponse from(TicketHistory history) {
        return new TicketHistoryResponse(
                history.getId(),
                history.getTicket().getId(),
                history.getChangedBy().getEmail(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getChangedAt()
        );
    }
}
