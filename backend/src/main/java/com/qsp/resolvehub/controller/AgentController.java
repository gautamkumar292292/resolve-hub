
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.request.CommentRequest;
import com.qsp.resolvehub.dto.response.AgentDashboardResponse;
import com.qsp.resolvehub.dto.response.CommentResponse;
import com.qsp.resolvehub.dto.response.TicketResponse;
import com.qsp.resolvehub.entity.Comment;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.service.AgentDashboardService;
import com.qsp.resolvehub.service.CommentService;
import com.qsp.resolvehub.service.TicketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgentController {

    private final TicketService ticketService;
    private final CommentService commentService;
    private final AgentDashboardService agentDashboardService;

    // Get tickets assigned to the authenticated agent.
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

    // Get one ticket assigned to this agent.
    @GetMapping("/agent/tickets/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<TicketResponse> getMyTicketById(
            @PathVariable Integer id,
            Authentication authentication) {

        Ticket ticket = ticketService.getMyTicketById(
                id, authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // Start an assigned ticket.
    @PatchMapping("/agent/tickets/{id}/start")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<TicketResponse> startTicket(
            @PathVariable Integer id,
            Authentication authentication) {

        Ticket ticket = ticketService.startTicket(
                id, authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // Resolve an in-progress ticket.
    @PatchMapping("/agent/tickets/{id}/resolve")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Integer id,
            Authentication authentication) {

        Ticket ticket = ticketService.resolveTicket(
                id, authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // Get comments for an accessible ticket.
    @GetMapping("/agent/tickets/{id}/comments")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Page<CommentResponse>> getTicketComments(
            @PathVariable Integer id,
            Authentication authentication,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        Page<CommentResponse> comments = commentService
                .getTicketComments(
                        id, authentication.getName(), pageable)
                .map(CommentResponse::from);

        return ResponseEntity.ok(comments);
    }

    // Add a comment.
    @PostMapping("/agent/tickets/{id}/comments")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Integer id,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {

        Comment comment = commentService.addComment(
                id,
                authentication.getName(),
                request.content()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommentResponse.from(comment));
    }

    // Agent dashboard.
    @GetMapping("/dashboard/agent")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<AgentDashboardResponse> getDashboard(
            Authentication authentication) {

        return ResponseEntity.ok(
                agentDashboardService.getDashboard(
                        authentication.getName())
        );
    }
}
