package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.dto.response.EmployeeDashboardResponse;
import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.repository.TicketRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.EmployeeDashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeDashboardServiceImpl
        implements EmployeeDashboardService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    @Override
    public EmployeeDashboardResponse getDashboard(String employeeEmail) {

        User employee = userRepository.findByEmail(employeeEmail)
                .orElseThrow(() ->
                        new AccessDeniedException("Employee not found"));

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new AccessDeniedException(
                    "Only employees can access this dashboard");
        }

        long totalTickets =
                ticketRepository.countByRequesterAndDeletedFalse(employee);

        long openTickets =
                ticketRepository.countByRequesterAndStatus(
                        employee, TicketStatus.OPEN);

        long assignedTickets =
                ticketRepository.countByRequesterAndStatus(
                        employee, TicketStatus.ASSIGNED);

        long inProgressTickets =
                ticketRepository.countByRequesterAndStatus(
                        employee, TicketStatus.IN_PROGRESS);

        long resolvedTickets =
                ticketRepository.countByRequesterAndStatus(
                        employee, TicketStatus.RESOLVED);

        long closedTickets =
                ticketRepository.countByRequesterAndStatus(
                        employee, TicketStatus.CLOSED);

        long reopenedTickets =
                ticketRepository.countByRequesterAndStatus(
                        employee, TicketStatus.REOPENED);

        return new EmployeeDashboardResponse(
                totalTickets,
                openTickets,
                assignedTickets,
                inProgressTickets,
                resolvedTickets,
                closedTickets,
                reopenedTickets
        );
    }
}