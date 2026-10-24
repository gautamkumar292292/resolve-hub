
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketService {

    // Get all tickets assigned to the logged-in agent
    Page<Ticket> getMyTickets(
            String agentEmail,
            Pageable pageable
    );

    // Get assigned tickets filtered by status
    Page<Ticket> getMyTicketsByStatus(
            String agentEmail,
            TicketStatus status,
            Pageable pageable
    );

    // Get a particular assigned ticket
    Ticket getMyTicketById(
            Integer ticketId,
            String agentEmail
    );

    // Start working on an assigned ticket
    Ticket startTicket(
            Integer ticketId,
            String agentEmail
    );

    // Resolve an assigned ticket
    Ticket resolveTicket(
            Integer ticketId,
            String agentEmail
    );

    Ticket assignTicket(Integer ticketId, Integer agentId);

    Page<Ticket> getAllTickets(
            TicketStatus status,
            Pageable pageable
    );

    Ticket getTicketById(Integer ticketId);

    Ticket updateTicketStatus(
            Integer ticketId,
            TicketStatus newStatus
    );
}
