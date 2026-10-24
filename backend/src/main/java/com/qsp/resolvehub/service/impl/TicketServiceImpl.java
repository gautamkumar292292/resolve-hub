
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.exception.TicketNotFoundException;
import com.qsp.resolvehub.repository.TicketRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.TicketService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    private User getAgent(String email) {
        User agent = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AccessDeniedException("Agent not found"));

        if (agent.getRole() != Role.AGENT) {
            throw new AccessDeniedException(
                    "Only agents can access this resource");
        }

        return agent;
    }

    private Ticket getAssignedTicket(Integer ticketId, String email) {
        User agent = getAgent(email);

        return ticketRepository.findByIdAndAssignedTo(ticketId, agent)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found or not assigned to you"
                        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getMyTickets(
            String agentEmail,
            Pageable pageable) {

        User agent = getAgent(agentEmail);
        return ticketRepository.findByAssignedTo(agent, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getMyTicketsByStatus(
            String agentEmail,
            TicketStatus status,
            Pageable pageable) {

        User agent = getAgent(agentEmail);
        return ticketRepository.findByAssignedToAndStatus(
                agent, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getMyTicketById(
            Integer ticketId,
            String agentEmail) {

        return getAssignedTicket(ticketId, agentEmail);
    }

    @Override
    public Ticket startTicket(
            Integer ticketId,
            String agentEmail) {

        Ticket ticket = getAssignedTicket(ticketId, agentEmail);

        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Only ASSIGNED tickets can be started");
        }

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket resolveTicket(
            Integer ticketId,
            String agentEmail) {

        Ticket ticket = getAssignedTicket(ticketId, agentEmail);

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Only IN_PROGRESS tickets can be resolved");
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(java.time.LocalDateTime.now());

        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket assignTicket(Integer ticketId, Integer agentId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with ID: " + ticketId
                        ));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() ->
                        new com.qsp.resolvehub.exception.UserNotFoundException(
                                "User not found with ID: " + agentId
                        ));

        if (agent.getRole() != Role.AGENT) {
            throw new IllegalArgumentException(
                    "Tickets can only be assigned to users with AGENT role"
            );
        }

        if (!agent.isAccount_status()) {
            throw new IllegalStateException(
                    "Cannot assign a ticket to an inactive agent"
            );
        }

        if (ticket.getStatus() != TicketStatus.OPEN
                && ticket.getStatus() != TicketStatus.REOPENED) {
            throw new IllegalStateException(
                    "Only OPEN or REOPENED tickets can be assigned"
            );
        }

        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.ASSIGNED);

        return ticketRepository.save(ticket);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getAllTickets(
            TicketStatus status,
            Pageable pageable) {

        if (status == null) {
            return ticketRepository.findAll(pageable);
        }

        return ticketRepository.findByStatus(status, pageable);
    }


    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketById(Integer ticketId) {

        return ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with ID: " + ticketId
                        ));
    }


    @Override
    public Ticket updateTicketStatus(
            Integer ticketId,
            TicketStatus newStatus) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with ID: " + ticketId
                        ));

        TicketStatus currentStatus = ticket.getStatus();

        boolean validTransition = switch (currentStatus) {
            case OPEN -> newStatus == TicketStatus.ASSIGNED
                    || newStatus == TicketStatus.CLOSED;

            case ASSIGNED -> newStatus == TicketStatus.IN_PROGRESS
                    || newStatus == TicketStatus.OPEN
                    || newStatus == TicketStatus.CLOSED;

            case IN_PROGRESS -> newStatus == TicketStatus.RESOLVED
                    || newStatus == TicketStatus.ASSIGNED
                    || newStatus == TicketStatus.CLOSED;

            case RESOLVED -> newStatus == TicketStatus.CLOSED
                    || newStatus == TicketStatus.REOPENED;

            case CLOSED -> newStatus == TicketStatus.REOPENED;

            case REOPENED -> newStatus == TicketStatus.ASSIGNED
                    || newStatus == TicketStatus.CLOSED;
        };

        if (currentStatus == newStatus || !validTransition) {
            throw new IllegalStateException(
                    "Invalid ticket status transition: "
                            + currentStatus + " -> " + newStatus
            );
        }

        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(java.time.LocalDateTime.now());
        } else if (newStatus == TicketStatus.REOPENED) {
            ticket.setResolvedAt(null);
        }

        return ticketRepository.save(ticket);
    }


    @Override
    public Ticket closeMyTicket(Integer ticketId, String employeeEmail) {
        Ticket ticket = getEmployeeTicket(ticketId, employeeEmail);

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Only RESOLVED tickets can be closed by employees");
        }

        ticket.setStatus(TicketStatus.CLOSED);

        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket reopenMyTicket(Integer ticketId, String employeeEmail) {
        Ticket ticket = getEmployeeTicket(ticketId, employeeEmail);

        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalStateException(
                    "Only CLOSED tickets can be reopened by employees");
        }

        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setResolvedAt(null);

        return ticketRepository.save(ticket);
    }



    private User getEmployee(String email) {
        User employee = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AccessDeniedException("Employee not found"));

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new AccessDeniedException(
                    "Only employees can perform this action");
        }

        if (!employee.isAccount_status()) {
            throw new AccessDeniedException(
                    "Inactive employees cannot perform this action");
        }

        return employee;
    }

    private Ticket getEmployeeTicket(
            Integer ticketId,
            String employeeEmail) {

        User employee = getEmployee(employeeEmail);

        return ticketRepository.findByIdAndRequester(ticketId, employee)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found or does not belong to you"));
    }

}
