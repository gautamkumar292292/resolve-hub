
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.response.TicketResponse;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.service.TicketService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgentController {

    private final TicketService ticketService;

    // AGENT: Get tickets assigned to the authenticated agent.
    @GetMapping("/tickets/assigned")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            Authentication authentication,
            @RequestParam(required = false) TicketStatus status,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable) {

        String agentEmail = authentication.getName();

        Page<TicketResponse> tickets;

        if (status == null) {
            tickets = ticketService
                    .getMyTickets(agentEmail, pageable)
                    .map(TicketResponse::from);
        } else {
            tickets = ticketService
                    .getMyTicketsByStatus(agentEmail, status, pageable)
                    .map(TicketResponse::from);
        }

        return ResponseEntity.ok(tickets);
    }

    // AGENT: Get one ticket assigned to this agent.
    @GetMapping("/agent/tickets/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<TicketResponse> getMyTicketById(
            @PathVariable Integer id,
            Authentication authentication) {

        Ticket ticket = ticketService.getMyTicketById(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // AGENT: Start an assigned ticket.
    // POST /api/tickets/{id}/start
    @PostMapping("/tickets/{id}/start")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<TicketResponse> startTicket(
            @PathVariable Integer id,
            Authentication authentication) {

        Ticket ticket = ticketService.startTicket(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // AGENT: Resolve an in-progress ticket.
    // POST /api/tickets/{id}/resolve
    @PostMapping("/tickets/{id}/resolve")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Integer id,
            Authentication authentication) {

        Ticket ticket = ticketService.resolveTicket(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }
}
