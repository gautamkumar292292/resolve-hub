package com.qsp.resolvehub.service;

import com.qsp.resolvehub.entity.Ticket;

public interface AutoAssignmentService {

    Ticket assignToLeastBusyAgent(Integer ticketId);

    Ticket assignToLeastBusyAgent(Integer ticketId, String adminEmail);
}