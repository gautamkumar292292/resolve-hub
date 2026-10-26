
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



    @GetMapping("/tickets/assigned")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            Authentication authentication,
            @RequestParam(required = false) TicketStatus status,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        Page<TicketResponse> tickets =
                ticketService.getMyTicketResponses(
                        authentication.getName(),
                        status,
                        pageable
                );

        return ResponseEntity.ok(tickets);
    }

    // AGENT: Get one ticket assigned to this agent.
    @GetMapping("/agent/tickets/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<TicketResponse> getMyTicketById(
            @PathVariable Integer id,
            Authentication authentication) {

        TicketResponse response =
                ticketService.getMyTicketResponseById(
                        id,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    // AGENT: Start an assigned ticket.
    // POST /api/tickets/{id}/start
    @PostMapping("/tickets/{id}/start")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<TicketResponse> startTicket(
            @PathVariable Integer id,
            Authentication authentication) {

        TicketResponse response = ticketService.startTicketResponse(
                id,
                authentication.getName()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tickets/{id}/resolve")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        TicketResponse response = ticketService.resolveTicketResponse(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }

}
