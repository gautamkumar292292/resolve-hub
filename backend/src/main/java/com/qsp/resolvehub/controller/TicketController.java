
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.service.TicketService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // EMPLOYEE: Close their own resolved ticket
    @PatchMapping("/employee/tickets/{ticketId}/close")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Ticket> closeMyTicket(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        Ticket ticket = ticketService.closeMyTicket(
                ticketId,
                authentication.getName()
        );

        return ResponseEntity.ok(ticket);
    }

    // EMPLOYEE: Reopen their own closed ticket
    @PatchMapping("/employee/tickets/{ticketId}/reopen")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Ticket> reopenMyTicket(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        Ticket ticket = ticketService.reopenMyTicket(
                ticketId,
                authentication.getName()
        );

        return ResponseEntity.ok(ticket);
    }

    // ADMIN: View all tickets, optionally filtered by status
    @GetMapping("/admin/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Ticket>> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(
                ticketService.getAllTickets(status, pageable)
        );
    }

    // ADMIN: View a specific ticket
    @GetMapping("/admin/tickets/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Ticket> getTicketById(
            @PathVariable Integer ticketId) {

        return ResponseEntity.ok(
                ticketService.getTicketById(ticketId)
        );
    }

    // ADMIN: Generic status updates
    @PatchMapping("/admin/tickets/{ticketId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Ticket> updateTicketStatus(
            @PathVariable Integer ticketId,
            @RequestParam TicketStatus status) {

        return ResponseEntity.ok(
                ticketService.updateTicketStatus(ticketId, status)
        );
    }
}
