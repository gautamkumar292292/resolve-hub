
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.exception.TicketNotFoundException;
import com.qsp.resolvehub.repository.TicketRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.AutoAssignmentService;
import com.qsp.resolvehub.service.TicketHistoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoAssignmentServiceImpl implements AutoAssignmentService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;

    @Override
    @Transactional
    public Ticket assignToLeastBusyAgent(Integer ticketId, String adminEmail){

        // Fetch and lock the ticket.
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with ID: " + ticketId
                        ));

        // Only OPEN tickets can be auto-assigned.
        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new IllegalStateException(
                    "Only OPEN tickets can be auto-assigned"
            );
        }

        // Capture the previous status for audit history.
        TicketStatus oldStatus = ticket.getStatus();

        // Retrieve active agents.
        List<User> agents =
                userRepository.findActiveAgentsForUpdate(Role.AGENT);

        if (agents.isEmpty()) {
            throw new IllegalStateException(
                    "No active agents are available"
            );
        }

        // Select the agent with the fewest active assigned tickets.
        User selectedAgent = agents.stream()
                .min(Comparator.comparingLong(agent ->
                        ticketRepository.countByAssignedToAndStatusIn(
                                agent,
                                List.of(
                                        TicketStatus.ASSIGNED,
                                        TicketStatus.IN_PROGRESS
                                )
                        )))
                .orElseThrow();

        // Assign the ticket.
        ticket.setAssignedTo(selectedAgent);
        ticket.setStatus(TicketStatus.ASSIGNED);

        Ticket savedTicket = ticketRepository.save(ticket);

        // Record the status change within the same transaction.
        ticketHistoryService.recordStatusChange(
                savedTicket,
                oldStatus,
                TicketStatus.ASSIGNED,
                adminEmail
        );

        return savedTicket;
    }
}
