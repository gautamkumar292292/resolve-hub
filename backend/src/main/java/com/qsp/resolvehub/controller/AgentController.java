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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final TicketService ticketService;
    private final CommentService commentService;
    private final AgentDashboardService agentDashboardService;

    // Get all assigned tickets, optionally filtered by status
    @GetMapping("/tickets")
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            Authentication authentication,
            @RequestParam(required = false) TicketStatus status,
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

    // Get one assigned ticket
    @GetMapping("/tickets/{id}")
    public ResponseEntity<TicketResponse> getMyTicketById(
            @PathVariable Integer id,
            Authentication authentication) {

        Ticket ticket = ticketService.getMyTicketById(
                id, authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // Start an assigned ticket
    @PatchMapping("/tickets/{id}/start")
    public ResponseEntity<TicketResponse> startTicket(
            @PathVariable Integer id,
            Authentication authentication) {

        Ticket ticket = ticketService.startTicket(
                id, authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // Resolve an in-progress ticket
    @PatchMapping("/tickets/{id}/resolve")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Integer id,
            Authentication authentication) {

        Ticket ticket = ticketService.resolveTicket(
                id, authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // Get comments for an assigned ticket
    @GetMapping("/tickets/{id}/comments")
    public ResponseEntity<Page<CommentResponse>> getTicketComments(
            @PathVariable Integer id,
            Authentication authentication,
            Pageable pageable) {

        Page<CommentResponse> comments = commentService
                .getTicketComments(
                        id, authentication.getName(), pageable)
                .map(CommentResponse::from);

        return ResponseEntity.ok(comments);
    }

    // Add a comment to an assigned ticket
    @PostMapping("/tickets/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Integer id,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {

        Comment comment = commentService.addComment(
                id,
                authentication.getName(),
                request.content()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommentResponse.from(comment));
    }

    // Get agent dashboard statistics
    @GetMapping("/dashboard")
    public ResponseEntity<AgentDashboardResponse> getDashboard(
            Authentication authentication) {

        return ResponseEntity.ok(
                agentDashboardService.getDashboard(
                        authentication.getName()
                )
        );
    }
}