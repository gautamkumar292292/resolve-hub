package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.dto.request.TicketRequest;
import com.qsp.resolvehub.dto.response.TicketResponse;
import com.qsp.resolvehub.entity.*;
import com.qsp.resolvehub.exception.ResourceNotFoundException;
import com.qsp.resolvehub.exception.TicketNotFoundException;
import com.qsp.resolvehub.exception.UserNotFoundException;
import com.qsp.resolvehub.repository.CategoryRepository;
import com.qsp.resolvehub.repository.TicketRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.TicketHistoryService;
import com.qsp.resolvehub.service.TicketService;
import com.qsp.resolvehub.specification.TicketSpecification;
import com.qsp.resolvehub.entity.TicketPriority;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.TicketPriority;


@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;
    private final CategoryRepository categoryRepository;
    // -------------------- Common helpers --------------------

    private User getAgent(String email) {
        User agent = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AccessDeniedException("Agent not found"));

        if (agent.getRole() != Role.AGENT) {
            throw new AccessDeniedException(
                    "Only agents can access this resource");
        }

        if (!agent.isAccount_status()) {
            throw new AccessDeniedException(
                    "Inactive agents cannot perform this action");
        }

        return agent;
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

    private Ticket getTicketOrThrow(Integer ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with ID: " + ticketId));
    }

    private Ticket getAssignedTicket(
            Integer ticketId,
            String agentEmail) {

        User agent = getAgent(agentEmail);

        return ticketRepository.findByIdAndAssignedTo(ticketId, agent)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found or not assigned to you"));
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

    private Category getActiveCategory(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        if (!category.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot use an inactive category");
        }

        return category;
    }

    private void recordStatusChange(
            Ticket ticket,
            TicketStatus oldStatus,
            String changedByEmail) {

        if (oldStatus != ticket.getStatus()) {
            ticketHistoryService.recordStatusChange(
                    ticket,
                    oldStatus,
                    ticket.getStatus(),
                    changedByEmail
            );
        }
    }

    private String generateTicketNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String ticketNumber = "TKT-" +
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 12)
                            .toUpperCase(Locale.ROOT);

            if (!ticketRepository.existsByTicketNumber(ticketNumber)) {
                return ticketNumber;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique ticket number");
    }

    // -------------------- Agent operations --------------------

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
    public Ticket startTicket(Integer ticketId, String agentEmail) {

        Ticket ticket = getAssignedTicket(ticketId, agentEmail);

        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Only ASSIGNED tickets can be started");
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        Ticket savedTicket = ticketRepository.save(ticket);
        recordStatusChange(savedTicket, oldStatus, agentEmail);

        return savedTicket;
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

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());

        Ticket savedTicket = ticketRepository.save(ticket);
        recordStatusChange(savedTicket, oldStatus, agentEmail);

        return savedTicket;
    }

    // -------------------- Admin operations --------------------

    @Override
    public Ticket assignTicket(
            Integer ticketId,
            Integer agentId,
            String adminEmail) {

        Ticket ticket = getTicketOrThrow(ticketId);

        User agent = userRepository.findById(agentId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with ID: " + agentId));

        if (agent.getRole() != Role.AGENT) {
            throw new IllegalArgumentException(
                    "Tickets can only be assigned to users with AGENT role");
        }

        if (!agent.isAccount_status()) {
            throw new IllegalStateException(
                    "Cannot assign a ticket to an inactive agent");
        }

        if (ticket.getStatus() != TicketStatus.OPEN
                && ticket.getStatus() != TicketStatus.REOPENED) {
            throw new IllegalStateException(
                    "Only OPEN or REOPENED tickets can be assigned");
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.ASSIGNED);

        Ticket savedTicket = ticketRepository.save(ticket);
        recordStatusChange(savedTicket, oldStatus, adminEmail);

        return savedTicket;
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
        return getTicketOrThrow(ticketId);
    }

    @Override
    public Ticket updateTicketStatus(
            Integer ticketId,
            TicketStatus newStatus,
            String changedByEmail) {

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "New ticket status cannot be null");
        }

        Ticket ticket = getTicketOrThrow(ticketId);
        TicketStatus oldStatus = ticket.getStatus();

        boolean validTransition = switch (oldStatus) {
            case OPEN ->
                    newStatus == TicketStatus.ASSIGNED;

            case ASSIGNED ->
                    newStatus == TicketStatus.IN_PROGRESS;

            case IN_PROGRESS ->
                    newStatus == TicketStatus.ASSIGNED
                            || newStatus == TicketStatus.RESOLVED;

            case RESOLVED ->
                    newStatus == TicketStatus.CLOSED
                            || newStatus == TicketStatus.REOPENED;

            case CLOSED ->
                    newStatus == TicketStatus.REOPENED;

            case REOPENED ->
                    newStatus == TicketStatus.ASSIGNED;
        };

        if (oldStatus == newStatus || !validTransition) {
            throw new IllegalStateException(
                    "Invalid ticket status transition: "
                            + oldStatus + " -> " + newStatus);
        }

        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (newStatus == TicketStatus.REOPENED
                || newStatus == TicketStatus.OPEN
                || newStatus == TicketStatus.ASSIGNED
                || newStatus == TicketStatus.IN_PROGRESS) {
            ticket.setResolvedAt(null);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        recordStatusChange(savedTicket, oldStatus, changedByEmail);

        return savedTicket;
    }

    // -------------------- Employee operations --------------------

    @Override
    public Ticket createTicket(
            TicketRequest request,
            String employeeEmail) {

        User employee = getEmployee(employeeEmail);
        Category category = getActiveCategory(request.categoryId());

        Ticket ticket = new Ticket();

        ticket.setPriority(request.priority());
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(category);
        ticket.setRequester(employee);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setAssignedTo(null);
        ticket.setResolvedAt(null);

        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getMyEmployeeTickets(
            String employeeEmail,
            Pageable pageable) {

        User employee = getEmployee(employeeEmail);
        return ticketRepository.findByRequester(employee, pageable);
    }

    private void validateTicketId(Integer ticketId) {
        if (ticketId == null || ticketId <= 0) {
            throw new IllegalArgumentException(
                    "Ticket ID must be a positive number");
        }
    }


    @Override
    @Transactional
    public Ticket updateMyTicket(
            Integer ticketId,
            TicketRequest request,
            String employeeEmail) {

        // Validate ticket ID
        if (ticketId == null || ticketId <= 0) {
            throw new IllegalArgumentException(
                    "Ticket ID must be a positive number"
            );
        }

        // Validate request
        if (request == null) {
            throw new IllegalArgumentException(
                    "Ticket update request cannot be null"
            );
        }

        // Verify employee and ticket ownership
        Ticket ticket = getEmployeeTicket(ticketId, employeeEmail);

        // Only OPEN tickets can be updated
        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new IllegalStateException(
                    "Only OPEN tickets can be edited"
            );
        }

        // Verify category exists and is active
        Category category = getActiveCategory(request.categoryId());

        // Update ticket fields
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(category);

        return ticketRepository.save(ticket);
    }


    @Override
    @Transactional
    public Ticket closeMyTicket(
            Integer ticketId,
            String employeeEmail) {

        // Validate ticket ID
        if (ticketId == null || ticketId <= 0) {
            throw new IllegalArgumentException(
                    "Ticket ID must be a positive number"
            );
        }

        // Verify employee and ticket ownership
        Ticket ticket = getEmployeeTicket(ticketId, employeeEmail);

        // Only RESOLVED tickets can be closed
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Only RESOLVED tickets can be closed by employees"
            );
        }

        // Capture old status for history
        TicketStatus oldStatus = ticket.getStatus();

        // Update status
        ticket.setStatus(TicketStatus.CLOSED);

        // Persist changes and record history
        Ticket savedTicket = ticketRepository.save(ticket);

        recordStatusChange(
                savedTicket,
                oldStatus,
                employeeEmail
        );

        return savedTicket;
    }



    @Override
    @Transactional
    public void deleteMyTicket(
            Integer ticketId,
            String employeeEmail) {

        // Validate ticket ID
        if (ticketId == null || ticketId <= 0) {
            throw new IllegalArgumentException(
                    "Ticket ID must be a valid number"
            );
        }

        // Verify employee and ticket ownership
        Ticket ticket = getEmployeeTicket(ticketId, employeeEmail);

        // Only OPEN, unassigned tickets can be deleted
        if (ticket.getStatus() != TicketStatus.OPEN
                || ticket.getAssignedTo() != null) {
            throw new IllegalStateException(
                    "Only OPEN, unassigned tickets can be deleted"
            );
        }

        ticketRepository.delete(ticket);
    }

    @Override
    public Ticket reopenMyTicket(Integer ticketId, String employeeEmail) {
        validateTicketId(ticketId);

        Ticket ticket = getEmployeeTicket(ticketId, employeeEmail);

        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalStateException(
                    "Only CLOSED tickets can be reopened by employees");
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setResolvedAt(null);

        Ticket savedTicket = ticketRepository.save(ticket);
        recordStatusChange(savedTicket, oldStatus, employeeEmail);

        return savedTicket;
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketForCurrentUser(
            Integer ticketId,
            String currentUserEmail) {

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (!user.isAccount_status()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Your account is inactive");
        }

        return switch (user.getRole()) {
            case EMPLOYEE ->
                    ticketRepository.findByIdAndRequester(ticketId, user)
                            .orElseThrow(() ->
                                    new TicketNotFoundException(
                                            "Ticket not found"));

            case AGENT ->
                    ticketRepository.findByIdAndAssignedTo(ticketId, user)
                            .orElseThrow(() ->
                                    new TicketNotFoundException(
                                            "Ticket not found"));

            case ADMIN -> ticketRepository.findById(ticketId)
                    .orElseThrow(() ->
                            new TicketNotFoundException(
                                    "Ticket not found"));
        };
    }


    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> searchTickets(
            String search,
            TicketStatus status,
            TicketPriority priority,
            Integer categoryId,
            Integer assignedTo,
            Pageable pageable,
            String currentUserEmail) {

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (!user.isAccount_status()) {
            throw new AccessDeniedException("Your account is inactive");
        }

        Specification<Ticket> specification =
                TicketSpecification.search(search)
                        .and(TicketSpecification.hasStatus(status))
                        .and(TicketSpecification.hasPriority(priority))
                        .and(TicketSpecification.hasCategory(categoryId));

        if (user.getRole() == Role.AGENT) {

            // Agents can only search their own assigned tickets.
            if (assignedTo != null && !assignedTo.equals(user.getId())) {
                return Page.empty(pageable);
            }

            specification = specification.and(
                    TicketSpecification.assignedTo(user.getId())
            );

        } else if (user.getRole() == Role.ADMIN) {

            // Admins can search tickets assigned to any agent.
            specification = specification.and(
                    TicketSpecification.assignedTo(assignedTo)
            );

        } else {
            throw new AccessDeniedException(
                    "Only admins and agents can search tickets"
            );
        }

        return ticketRepository.findAll(specification, pageable)
                .map(TicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getMyEmployeeTicketById(
            Integer ticketId,
            String employeeEmail) {

        if (ticketId == null || ticketId <= 0) {
            throw new IllegalArgumentException(
                    "Ticket ID must be a positive number");
        }

        User employee = getEmployee(employeeEmail);

        return ticketRepository.findByIdAndRequester(ticketId, employee)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found or does not belong to you"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> searchTicketsByTitle(
            String title,
            Pageable pageable) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Title search term cannot be blank");
        }

        return ticketRepository.findByTitleContainingIgnoreCase(
                title.trim(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getTicketsByPriority(
            TicketPriority priority,
            Pageable pageable) {

        if (priority == null) {
            throw new IllegalArgumentException(
                    "Ticket priority cannot be null");
        }

        return ticketRepository.findByPriority(priority, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTicketsAssignedTo(Integer agentId) {

        if (agentId == null || agentId <= 0) {
            throw new IllegalArgumentException(
                    "Agent ID must be a positive number");
        }

        User agent = userRepository.findById(agentId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with ID: " + agentId));

        if (agent.getRole() != Role.AGENT) {
            throw new IllegalArgumentException(
                    "The specified user is not an agent");
        }

        if (!agent.isAccount_status()) {
            throw new IllegalStateException(
                    "Cannot retrieve statistics for an inactive agent");
        }

        return ticketRepository.countByAssignedTo(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketByTicketNumber(String ticketNumber) {

        if (ticketNumber == null || ticketNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticket number cannot be blank");
        }

        return ticketRepository.findByTicketNumber(ticketNumber.trim())
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with number: " + ticketNumber));
    }

    

    @Override
    public Ticket unassignTicket(Integer ticketId, String adminEmail) {
        validateTicketId(ticketId);

        Ticket ticket = getTicketOrThrow(ticketId);

        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Only ASSIGNED tickets can be unassigned"
            );
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setAssignedTo(null);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setResolvedAt(null);

        Ticket savedTicket = ticketRepository.save(ticket);

        recordStatusChange(savedTicket, oldStatus, adminEmail);

        return savedTicket;
    }

}
