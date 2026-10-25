
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.response.TicketHistoryResponse;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.repository.TicketRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.TicketHistoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketHistoryController {

    private final TicketHistoryService ticketHistoryService;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @GetMapping("/{ticketId}/history")
    public ResponseEntity<List<TicketHistoryResponse>> getTicketHistory(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AccessDeniedException("User not found"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new com.qsp.resolvehub.exception.TicketNotFoundException(
                                "Ticket not found with id: " + ticketId));

        boolean allowed = switch (user.getRole()) {
            case ADMIN -> true;

            case AGENT ->
                    ticket.getAssignedTo() != null
                            && ticket.getAssignedTo().getId()
                            .equals(user.getId());

            case EMPLOYEE ->
                    ticket.getRequester() != null
                            && ticket.getRequester().getId()
                            .equals(user.getId());
        };

        if (!allowed) {
            throw new AccessDeniedException(
                    "You are not allowed to view this ticket's history");
        }

        List<TicketHistoryResponse> history =
                ticketHistoryService.getTicketHistory(ticketId);

        return ResponseEntity.ok(history);
    }
}
