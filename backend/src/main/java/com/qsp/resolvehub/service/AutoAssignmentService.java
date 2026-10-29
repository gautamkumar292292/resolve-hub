package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.response.TicketResponse;

public interface AutoAssignmentService {

    TicketResponse assignToLeastBusyAgent(
            Integer ticketId,
            String adminEmail
    );
}