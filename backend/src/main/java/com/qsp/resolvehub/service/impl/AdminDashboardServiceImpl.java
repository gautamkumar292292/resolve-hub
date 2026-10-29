
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.dto.response.AdminDashboardResponse;
import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.repository.TicketRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.AdminDashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    @Override
    public AdminDashboardResponse getDashboard() {

        long totalUsers = userRepository.count();
        long totalAgents = userRepository.countByRoles(Role.AGENT);
        long totalEmployees = userRepository.countByRoles(Role.EMPLOYEE);

        // Count only active tickets (exclude soft-deleted tickets).
        long totalTickets = ticketRepository.countByDeletedFalse();

        // OPEN includes both OPEN and REOPENED tickets.
        long openTickets = ticketRepository.countByStatusInAndDeletedFalse(
                List.of(TicketStatus.OPEN, TicketStatus.REOPENED)
        );

        long assignedTickets =
                ticketRepository.countByAssignedToIsNotNullAndDeletedFalse();

        long inProgressTickets =
                ticketRepository.countByStatusAndDeletedFalse(
                        TicketStatus.IN_PROGRESS
                );

        long resolvedTickets =
                ticketRepository.countByStatusAndDeletedFalse(
                        TicketStatus.RESOLVED
                );

        long closedTickets =
                ticketRepository.countByStatusAndDeletedFalse(
                        TicketStatus.CLOSED
                );

        long unassignedTickets =
                ticketRepository.countByAssignedToIsNullAndDeletedFalse();

        return new AdminDashboardResponse(
                totalUsers,
                totalAgents,
                totalEmployees,
                totalTickets,
                openTickets,
                assignedTickets,
                inProgressTickets,
                resolvedTickets,
                closedTickets,
                unassignedTickets
        );
    }
}
