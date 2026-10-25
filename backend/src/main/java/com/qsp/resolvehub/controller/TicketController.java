
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.response.TicketResponse;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.service.TicketService;

import lombok.RequiredArgsConstructor;

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
    public ResponseEntity<TicketResponse> closeMyTicket(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        Ticket ticket = ticketService.closeMyTicket(
                ticketId,
                authentication.getName()
        );

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // EMPLOYEE: Reopen their own closed ticket
    @PatchMapping("/employee/tickets/{ticketId}/reopen")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TicketResponse> reopenMyTicket(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        Ticket ticket = ticketService.reopenMyTicket(
                ticketId,
                authentication.getName()
        );

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }
}
