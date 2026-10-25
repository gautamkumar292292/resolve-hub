
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.request.TicketRequest;
import com.qsp.resolvehub.dto.response.TicketResponse;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketPriority;
import com.qsp.resolvehub.entity.TicketStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketService {

    // Agent operations
    Page<Ticket> getMyTickets(
            String agentEmail,
            Pageable pageable
    );

    Page<Ticket> getMyTicketsByStatus(
            String agentEmail,
            TicketStatus status,
            Pageable pageable
    );

    Ticket getMyTicketById(
            Integer ticketId,
            String agentEmail
    );

    Ticket startTicket(
            Integer ticketId,
            String agentEmail
    );

    Ticket resolveTicket(
            Integer ticketId,
            String agentEmail
    );

    // Admin operations
    Page<Ticket> getAllTickets(
            TicketStatus status,
            Pageable pageable
    );

    Ticket getTicketById(Integer ticketId);

    Ticket updateTicketStatus(
            Integer ticketId,
            TicketStatus newStatus,
            String changedByEmail
    );

    Ticket assignTicket(
            Integer ticketId,
            Integer agentId,
            String adminEmail
    );

    Ticket unassignTicket(
            Integer ticketId,
            String adminEmail
    );

    // Employee operations
    Ticket createTicket(
            TicketRequest request,
            String employeeEmail
    );

    Page<Ticket> getMyEmployeeTickets(
            String employeeEmail,
            Pageable pageable
    );

    Ticket getMyEmployeeTicketById(
            Integer ticketId,
            String employeeEmail
    );

    Ticket updateMyTicket(
            Integer ticketId,
            TicketRequest request,
            String employeeEmail
    );

    void deleteMyTicket(
            Integer ticketId,
            String employeeEmail
    );

    Ticket closeMyTicket(
            Integer ticketId,
            String employeeEmail
    );

    Ticket reopenMyTicket(
            Integer ticketId,
            String employeeEmail
    );

    Ticket getTicketForCurrentUser(
            Integer ticketId,
            String currentUserEmail
    );

    // Search and filtering
    Page<TicketResponse> searchTickets(
            String search,
            TicketStatus status,
            TicketPriority priority,
            Integer categoryId,
            Integer assignedTo,
            Pageable pageable,
            String currentUserEmail
    );

    Page<Ticket> searchTicketsByTitle(
            String title,
            Pageable pageable
    );

    Page<Ticket> getTicketsByPriority(
            TicketPriority priority,
            Pageable pageable
    );

    // Agent statistics
    long countTicketsAssignedTo(Integer agentId);

    // Ticket lookup
    Ticket getTicketByTicketNumber(String ticketNumber);
}
