
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

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;
    private final CategoryRepository categoryRepository;

    // -------------------- Common helpers --------------------

    private void validateTicketId(Integer ticketId) {
        if (ticketId == null || ticketId <= 0) {
            throw new IllegalArgumentException(
                    "Ticket ID must be a positive number"
            );
        }
    }

    private User getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException(
                    "Authenticated user email is required"
            );
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );
    }

    private User getAgent(String email) {
        User agent = getUserByEmail(email);

        if (agent.getRole() != Role.AGENT) {
            throw new AccessDeniedException(
                    "Only agents can perform this action"
            );
        }

        if (!agent.isEnabled()) {
            throw new AccessDeniedException(
                    "Inactive agents cannot perform this action"
            );
        }

        return agent;
    }

    private User getEmployee(String email) {
        User employee = getUserByEmail(email);

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new AccessDeniedException(
                    "Only employees can perform this action"
            );
        }

        if (!employee.isEnabled()) {
            throw new AccessDeniedException(
                    "Inactive employees cannot perform this action"
            );
        }

        return employee;
    }

    private Ticket getTicketOrThrow(Integer ticketId) {
        validateTicketId(ticketId);

        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with ID: " + ticketId));
    }

    private Ticket getAssignedTicket(
            Integer ticketId,
            String agentEmail) {

        validateTicketId(ticketId);
        User agent = getAgent(agentEmail);

        return ticketRepository
                .findByIdAndAssignedToAndDeletedFalse(ticketId, agent)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found or not assigned to you"));
    }

    private Category getActiveCategory(Integer categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException(
                    "Category is required"
            );
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found"
                        )
                );

        if (!category.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot use an inactive category"
            );
        }

        return category;
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
                "Could not generate a unique ticket number"
        );
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

    private void validateTicketRequest(TicketRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Ticket request cannot be null"
            );
        }

        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException(
                    "Title is required"
            );
        }

        if (request.description() == null
                || request.description().isBlank()) {
            throw new IllegalArgumentException(
                    "Description is required"
            );
        }

        if (request.priority() == null) {
            throw new IllegalArgumentException(
                    "Priority is required"
            );
        }

        if (request.categoryId() == null) {
            throw new IllegalArgumentException(
                    "Category is required"
            );
        }
    }

    // -------------------- Agent operations --------------------

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getMyTickets(
            String agentEmail,
            Pageable pageable) {

        User agent = getAgent(agentEmail);

        return ticketRepository.findByAssignedToAndDeletedFalse(
                agent, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getMyTicketsByStatus(
            String agentEmail,
            TicketStatus status,
            Pageable pageable) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Ticket status is required");
        }

        User agent = getAgent(agentEmail);

        return ticketRepository.findByAssignedToAndStatusAndDeletedFalse(
                agent, status, pageable);
    }

    private Ticket getEmployeeTicket(
            Integer ticketId,
            String employeeEmail) {

        validateTicketId(ticketId);
        User employee = getEmployee(employeeEmail);

        return ticketRepository
                .findByIdAndRequesterAndDeletedFalse(ticketId, employee)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found or does not belong to you"));
    }
// -------------------- Agent operations --------------------

    @Override
    @Transactional(readOnly = true)
    public Ticket getMyTicketById(
            Integer ticketId,
            String agentEmail) {

        return getAssignedTicket(ticketId, agentEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketById(Integer ticketId) {
        return getTicketOrThrow(ticketId);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketResponseById(
            Integer ticketId,
            String currentUserEmail) {

        Ticket ticket = getTicketForCurrentUser(
                ticketId,
                currentUserEmail
        );

        return TicketResponse.from(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getMyTicketResponses(
            String agentEmail,
            TicketStatus status,
            Pageable pageable) {

        Page<Ticket> tickets;

        if (status == null) {
            tickets = getMyTickets(agentEmail, pageable);
        } else {
            tickets = getMyTicketsByStatus(agentEmail, status, pageable);
        }

        return tickets.map(TicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getMyTicketResponseById(
            Integer ticketId,
            String agentEmail) {

        Ticket ticket = getMyTicketById(ticketId, agentEmail);

        return TicketResponse.from(ticket);
    }

    @Override
    @Transactional
    public TicketResponse startTicketResponse(
            Integer ticketId,
            String agentEmail
    ) {
        Ticket ticket = startTicket(ticketId, agentEmail);
        return TicketResponse.from(ticket);
    }

    @Override
    @Transactional
    public TicketResponse resolveTicketResponse(
            Integer ticketId,
            String agentEmail
    ) {
        Ticket ticket = resolveTicket(ticketId, agentEmail);
        return TicketResponse.from(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getMyEmployeeTicketResponses(
            String employeeEmail,
            Pageable pageable) {

        return getMyEmployeeTickets(employeeEmail, pageable)
                .map(TicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getMyEmployeeTicketResponseById(
            Integer ticketId,
            String employeeEmail) {

        Ticket ticket = getMyEmployeeTicketById(
                ticketId, employeeEmail);

        return TicketResponse.from(ticket);
    }

    @Override
    @Transactional
    public TicketResponse assignTicketResponse(
            Integer ticketId,
            Integer agentId,
            String adminEmail) {

        Ticket ticket = assignTicket(
                ticketId,
                agentId,
                adminEmail
        );

        return TicketResponse.from(ticket);
    }
    @Override
    @Transactional
    public TicketResponse closeMyTicketResponse(
            Integer ticketId,
            String employeeEmail) {

        Ticket ticket = closeMyTicket(ticketId, employeeEmail);
        return TicketResponse.from(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTicketResponsesByPriority(
            TicketPriority priority,
            Pageable pageable) {

        Page<Ticket> tickets =
                getTicketsByPriority(priority, pageable);

        return tickets.map(TicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTicketResponsesByStatus(
            TicketStatus status,
            Pageable pageable) {

        Page<Ticket> tickets =
                ticketRepository.findByStatusAndDeletedFalse(
                        status, pageable
                );

        return tickets.map(TicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTicketResponsesByCategory(
            Integer categoryId,
            Pageable pageable) {

        Page<Ticket> tickets =
                ticketRepository.findByCategoryIdAndDeletedFalse(
                        categoryId, pageable
                );

        return tickets.map(TicketResponse::from);
    }


    @Override
    public Ticket startTicket(
            Integer ticketId,
            String agentEmail) {

        Ticket ticket = getAssignedTicket(ticketId, agentEmail);

        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Only ASSIGNED tickets can be started"
            );
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, oldStatus, agentEmail);

        return saved;
    }

    @Override
    public Ticket resolveTicket(
            Integer ticketId,
            String agentEmail) {

        Ticket ticket = getAssignedTicket(ticketId, agentEmail);

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Only IN_PROGRESS tickets can be resolved"
            );
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, oldStatus, agentEmail);

        return saved;
    }

    // -------------------- Admin operations --------------------

    @Override
    public Ticket assignTicket(
            Integer ticketId,
            Integer agentId,
            String adminEmail) {

        User admin = getUserByEmail(adminEmail);

        if (admin.getRole() != Role.ADMIN || !admin.isEnabled()) {
            throw new AccessDeniedException(
                    "Only active admins can assign tickets"
            );
        }

        Ticket ticket = getTicketOrThrow(ticketId);

        System.out.println("DEBUG assignTicket: id=" + ticket.getId()
                + ", status=" + ticket.getStatus()
                + ", assignedTo=" + ticket.getAssignedTo());

        if (agentId == null || agentId <= 0) {
            throw new IllegalArgumentException(
                    "Agent ID must be a positive number");
        }

        User agent = userRepository.findById(agentId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with ID: " + agentId
                        )
                );

        if (agent.getRole() != Role.AGENT || !agent.isEnabled()) {
            throw new IllegalArgumentException(
                    "Ticket must be assigned to an active agent"
            );
        }

        if (ticket.getStatus() != TicketStatus.OPEN
                && ticket.getStatus() != TicketStatus.REOPENED) {
            throw new IllegalStateException(
                    "Only OPEN or REOPENED tickets can be assigned"
            );
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.ASSIGNED);
        ticket.setResolvedAt(null);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, oldStatus, adminEmail);

        return saved;
    }

    @Override
    @Transactional
    public void deleteTicketAsAdmin(
            Integer ticketId,
            String adminEmail) {

        validateTicketId(ticketId);

        User admin = getUserByEmail(adminEmail);

        if (admin.getRole() != Role.ADMIN || !admin.isEnabled()) {
            throw new AccessDeniedException(
                    "Only active admins can delete tickets");
        }

        Ticket ticket = getTicketOrThrow(ticketId);

        if (ticket.getStatus() != TicketStatus.OPEN
                || ticket.getAssignedTo() != null) {
            throw new IllegalStateException(
                    "Only OPEN, unassigned tickets can be deleted");
        }

        ticket.setDeleted(true);
        ticketRepository.save(ticket);
    }


    @Override
    @Transactional
    public void deleteMyTicket(
            Integer ticketId,
            String employeeEmail) {

        Ticket ticket = getEmployeeTicket(ticketId, employeeEmail);

        if (ticket.getStatus() != TicketStatus.OPEN
                && ticket.getStatus() != TicketStatus.REOPENED) {
            throw new IllegalStateException(
                    "Only OPEN or REOPENED tickets can be assigned"
            );
        }

        ticket.setDeleted(true);
        ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public TicketResponse unassignTicket(
            Integer ticketId,
            String adminEmail) {


        User admin = getUserByEmail(adminEmail);

        if (admin.getRole() != Role.ADMIN || !admin.isEnabled()) {
            throw new AccessDeniedException(
                    "Only active admins can unassign tickets"
            );
        }

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

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, oldStatus, adminEmail);

        return TicketResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getAllTickets(
            TicketStatus status,
            Pageable pageable) {

        if (status == null) {
            return ticketRepository.findByDeletedFalse(pageable);
        }

        return ticketRepository.findByStatusAndDeletedFalse(
                status, pageable);
    }

    /**
     * Handles workflow transitions that do not require assignment changes.
     *
     * OPEN/REOPENED -> ASSIGNED must go through assignTicket().
     * ASSIGNED -> OPEN must go through unassignTicket().
     */
    @Override
    @Transactional
    public Ticket updateTicketStatus(
            Integer ticketId,
            TicketStatus newStatus,
            String changedByEmail) {

        validateTicketId(ticketId);

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "New ticket status cannot be null");
        }

        User actor = getUserByEmail(changedByEmail);

        if (actor.getRole() != Role.ADMIN || !actor.isEnabled()) {
            throw new AccessDeniedException(
                    "Only active admins can use this status operation");
        }

        Ticket ticket = getTicketOrThrow(ticketId);
        TicketStatus oldStatus = ticket.getStatus();

        // Agent-only transitions must use the dedicated workflow endpoints.
        boolean validTransition = switch (oldStatus) {
            case ASSIGNED, IN_PROGRESS, OPEN, REOPENED -> false;

            case RESOLVED ->
                    newStatus == TicketStatus.CLOSED
                            || newStatus == TicketStatus.REOPENED;

            case CLOSED ->
                    newStatus == TicketStatus.REOPENED;
        };

        if (!validTransition) {
            throw new IllegalStateException(
                    "Invalid ticket status transition: "
                            + oldStatus + " -> " + newStatus
                            + ". Agent workflow transitions must use "
                            + "the dedicated start/resolve endpoints.");
        }

        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.REOPENED) {
            // Return the ticket to the assignment queue.
            ticket.setResolvedAt(null);
            ticket.setAssignedTo(null);
        } else if (newStatus == TicketStatus.CLOSED) {
            // Preserve the resolution timestamp for audit purposes.
        }

        Ticket saved = ticketRepository.save(ticket);

        recordStatusChange(saved, oldStatus, changedByEmail);

        return saved;
    }

    // -------------------- Employee operations --------------------

    @Override
    public Ticket createTicket(
            TicketRequest request,
            String employeeEmail) {

        validateTicketRequest(request);

        User employee = getEmployee(employeeEmail);
        Category category = getActiveCategory(request.categoryId());

        Ticket ticket = new Ticket();

        ticket.setTicketNumber(generateTicketNumber());
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(category);
        ticket.setRequester(employee);
        ticket.setPriority(request.priority());
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

        return ticketRepository.findByRequesterAndDeletedFalse(
                employee, pageable);
    }

    @Override
    @Transactional
    public Ticket updateMyTicket(
            Integer ticketId,
            TicketRequest request,
            String employeeEmail) {

        validateTicketId(ticketId);
        validateTicketRequest(request);

        Ticket ticket = getEmployeeTicket(ticketId, employeeEmail);

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new IllegalStateException(
                    "Only OPEN tickets can be edited"
            );
        }

        Category category = getActiveCategory(request.categoryId());

        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(category);
        ticket.setPriority(request.priority());

        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public Ticket closeMyTicket(Integer ticketId, String actorEmail) {
        validateTicketId(ticketId);

        User actor = getUserByEmail(actorEmail);

        if (!actor.isEnabled()) {
            throw new AccessDeniedException("Inactive users cannot close tickets");
        }

        Ticket ticket;

        if (actor.getRole() == Role.EMPLOYEE) {
            ticket = getEmployeeTicket(ticketId, actorEmail);
        } else if (actor.getRole() == Role.ADMIN) {
            ticket = getTicketOrThrow(ticketId);
        } else {
            throw new AccessDeniedException(
                    "Only employees and admins can close tickets"
            );
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Only RESOLVED tickets can be closed"
            );
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.CLOSED);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, oldStatus, actorEmail);

        return saved;
    }

    @Override
    public Ticket reopenMyTicket(
            Integer ticketId,
            String employeeEmail) {

        Ticket ticket = getEmployeeTicket(ticketId, employeeEmail);

        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalStateException(
                    "Only CLOSED tickets can be reopened by employees"
            );
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setResolvedAt(null);
        ticket.setAssignedTo(null);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, oldStatus, employeeEmail);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketForCurrentUser(
            Integer ticketId,
            String currentUserEmail) {

        validateTicketId(ticketId);

        User user = getUserByEmail(currentUserEmail);

        if (!user.isEnabled()) {
            throw new AccessDeniedException(
                    "Your account is inactive");
        }

        return switch (user.getRole()) {
            case EMPLOYEE ->
                    ticketRepository
                            .findByIdAndRequesterAndDeletedFalse(
                                    ticketId, user)
                            .orElseThrow(() ->
                                    new TicketNotFoundException(
                                            "Ticket not found"));

            case AGENT ->
                    ticketRepository
                            .findByIdAndAssignedToAndDeletedFalse(
                                    ticketId, user)
                            .orElseThrow(() ->
                                    new TicketNotFoundException(
                                            "Ticket not found"));

            case ADMIN -> getTicketOrThrow(ticketId);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getMyEmployeeTicketById(
            Integer ticketId,
            String employeeEmail) {

        return getEmployeeTicket(ticketId, employeeEmail);
    }

    // -------------------- Search and filtering --------------------

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

        User user = getUserByEmail(currentUserEmail);

        if (!user.isEnabled()) {
            throw new AccessDeniedException(
                    "Your account is inactive"
            );
        }

        if (user.getRole() != Role.ADMIN
                && user.getRole() != Role.AGENT) {
            throw new AccessDeniedException(
                    "Only admins and agents can search tickets"
            );
        }

        Specification<Ticket> specification =
                TicketSpecification.search(search)
                        .and(TicketSpecification.hasStatus(status))
                        .and(TicketSpecification.hasPriority(priority))
                        .and(TicketSpecification.hasCategory(categoryId))
                        .and((root, query, cb)
                                -> cb.isFalse(root.get("deleted")));

        
        if (user.getRole() == Role.AGENT) {
            if (assignedTo != null
                    && !assignedTo.equals(user.getId())) {
                return Page.empty(pageable);
            }

            specification = specification.and(
                    TicketSpecification.assignedTo(user.getId())
            );
        } else {
            specification = specification.and(
                    TicketSpecification.assignedTo(assignedTo)
            );
        }

        return ticketRepository.findAll(specification, pageable)
                .map(TicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> searchTicketsByTitle(
            String title,
            Pageable pageable) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Title search term cannot be blank"
            );
        }

        return ticketRepository
                .findByTitleContainingIgnoreCaseAndDeletedFalse(
                        title.trim(),
                        pageable
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> searchTicketResponsesByTitle(
            String title,
            Pageable pageable) {

        Page<Ticket> tickets = searchTicketsByTitle(title, pageable);


        return tickets.map(TicketResponse::from);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getTicketsByPriority(
            TicketPriority priority,
            Pageable pageable) {

        if (priority == null) {
            throw new IllegalArgumentException(
                    "Ticket priority cannot be null"
            );
        }

        return ticketRepository.findByPriorityAndDeletedFalse(
                priority, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTicketsAssignedTo(Integer agentId) {

        if (agentId == null || agentId <= 0) {
            throw new IllegalArgumentException(
                    "Agent ID must be a positive number"
            );
        }

        User agent = userRepository.findById(agentId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with ID: " + agentId
                        )
                );

        if (agent.getRole() != Role.AGENT) {
            throw new IllegalArgumentException(
                    "The specified user is not an agent"
            );
        }

        if (!agent.isEnabled()) {
            throw new IllegalStateException(
                    "Cannot retrieve statistics for an inactive agent"
            );
        }

        return ticketRepository.countByAssignedToAndDeletedFalse(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketByTicketNumber(String ticketNumber) {

        if (ticketNumber == null || ticketNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticket number cannot be blank"
            );
        }

        return ticketRepository
                .findByTicketNumberAndDeletedFalse(ticketNumber.trim())
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with number: "
                                        + ticketNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketResponseByTicketNumber(String ticketNumber) {

        if (ticketNumber == null || ticketNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticket number cannot be blank"
            );
        }

        Ticket ticket = ticketRepository
                .findByTicketNumberAndDeletedFalse(ticketNumber.trim())
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found with number: " + ticketNumber
                        ));

        return TicketResponse.from(ticket);
    }

}
