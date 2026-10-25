
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.response.TicketHistoryResponse;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketHistory;
import com.qsp.resolvehub.entity.TicketStatus;

import java.util.List;

public interface TicketHistoryService {

    TicketHistory recordStatusChange(
            Ticket ticket,
            TicketStatus oldStatus,
            TicketStatus newStatus,
            String changedByEmail
    );

    List<TicketHistoryResponse> getTicketHistory(
            Integer ticketId
    );
}
