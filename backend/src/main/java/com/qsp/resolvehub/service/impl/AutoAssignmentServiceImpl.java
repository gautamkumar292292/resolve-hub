
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.dto.response.TicketResponse;
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
    public TicketResponse assignToLeastBusyAgent(
            Integer ticketId,
            String adminEmail) {

        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with ID: " + ticketId
                        ));

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new IllegalStateException(
                    "Only OPEN tickets can be auto-assigned"
            );
        }

        TicketStatus oldStatus = ticket.getStatus();

        List<User> agents =
                userRepository.findActiveAgentsForUpdate(Role.AGENT);

        if (agents.isEmpty()) {
            throw new IllegalStateException(
                    "No active agents are available"
            );
        }

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

        ticket.setAssignedTo(selectedAgent);
        ticket.setStatus(TicketStatus.ASSIGNED);

        Ticket savedTicket = ticketRepository.save(ticket);

        ticketHistoryService.recordStatusChange(
                savedTicket,
                oldStatus,
                TicketStatus.ASSIGNED,
                adminEmail
        );

        return TicketResponse.from(savedTicket);
    }
}
