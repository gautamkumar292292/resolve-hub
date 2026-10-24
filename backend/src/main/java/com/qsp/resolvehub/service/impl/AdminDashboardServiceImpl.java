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

        long totalTickets = ticketRepository.count();

        long openTickets =
                ticketRepository.countByStatus(TicketStatus.OPEN);

        long assignedTickets =
                ticketRepository.countByAssignedToIsNotNull();

        long inProgressTickets =
                ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);

        long resolvedTickets =
                ticketRepository.countByStatus(TicketStatus.RESOLVED);

        long closedTickets =
                ticketRepository.countByStatus(TicketStatus.CLOSED);

        long unassignedTickets =
                ticketRepository.countByAssignedToIsNull();

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