package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.request.UpdateRoleRequest;
import com.qsp.resolvehub.dto.request.UpdateTicketStatusRequest;
import com.qsp.resolvehub.dto.request.UpdateUserRequest;
import com.qsp.resolvehub.dto.response.AgentWorkloadResponse;
import com.qsp.resolvehub.dto.response.TicketResponse;
import com.qsp.resolvehub.dto.response.UserResponse;
import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.service.AgentWorkloadService;
import com.qsp.resolvehub.service.AutoAssignmentService;
import com.qsp.resolvehub.service.TicketService;
import com.qsp.resolvehub.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final TicketService ticketService;
    private final AgentWorkloadService agentWorkloadService;
    private final AutoAssignmentService autoAssignmentService;


    // Get the currently authenticated admin.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentAdmin(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(UserResponse.from(user));
    }

    // Get all users.
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Get a user by ID.
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(userService.getUserById(id));
    }

    // Update a user's details.
    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(
                userService.adminUpdateUser(id, request));
    }

    // Update a user's role.
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoleRequest request) {

        return ResponseEntity.ok(
                userService.updateUserRole(id, request));
    }

    // Activate a user (existing endpoint).
    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                userService.setUserStatus(id, true));
    }

    // Deactivate a user (existing endpoint).
    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                userService.setUserStatus(id, false));
    }

    // Set a user's status using the requirements' status endpoint.
    // PATCH /api/admin/users/{id}/status?enabled=true|false
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Integer id,
            @RequestParam boolean enabled) {

        return ResponseEntity.ok(
                userService.setUserStatus(id, enabled));
    }

    // ADMIN: Delete an eligible ticket.
// DELETE /api/admin/tickets/{ticketId}
    @DeleteMapping("/tickets/{ticketId}")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        ticketService.deleteTicketAsAdmin(
                ticketId,
                authentication.getName());

        return ResponseEntity.noContent().build();
    }

    // Get all agents.
    @GetMapping("/agents")
    public ResponseEntity<List<UserResponse>> getAllAgents() {
        List<UserResponse> agents = userService.getAllUsers()
                .stream()
                .filter(user -> user.getRoles() == Role.AGENT)
                .toList();

        return ResponseEntity.ok(agents);
    }

    // Get all employees.
    @GetMapping("/employees")
    public ResponseEntity<List<UserResponse>> getAllEmployees() {
        List<UserResponse> employees = userService.getAllUsers()
                .stream()
                .filter(user -> user.getRoles() == Role.EMPLOYEE)
                .toList();

        return ResponseEntity.ok(employees);
    }

    // List tickets with optional status filtering and pagination.
    @GetMapping("/tickets")
    public ResponseEntity<Page<TicketResponse>> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<TicketResponse> tickets = ticketService
                .getAllTickets(status, pageable)
                .map(TicketResponse::from);

        return ResponseEntity.ok(tickets);
    }

    // Get a ticket by ID.
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable Integer ticketId) {

        return ResponseEntity.ok(
                TicketResponse.from(
                        ticketService.getTicketById(ticketId)));
    }

    // Update ticket status.
    @PatchMapping("/tickets/{ticketId}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Integer ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            Authentication authentication) {

        Ticket ticket = ticketService.updateTicketStatus(
                ticketId,
                request.status(),
                authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // Manually assign a ticket to an agent.
    @PostMapping("/tickets/{ticketId}/assign")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Integer ticketId,
            @RequestParam Integer agentId,
            @AuthenticationPrincipal User admin) {

        Ticket ticket = ticketService.assignTicket(
                ticketId,
                agentId,
                admin.getEmail());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // Remove the current agent assignment from a ticket.
    @PostMapping("/tickets/{ticketId}/unassign")
    public ResponseEntity<TicketResponse> unassignTicket(
            @PathVariable Integer ticketId,
            @AuthenticationPrincipal User admin) {

        Ticket ticket = ticketService.unassignTicket(
                ticketId,
                admin.getEmail());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // Get agent workload information.
    @GetMapping("/agents/workload")
    public ResponseEntity<List<AgentWorkloadResponse>> getAgentWorkloads() {
        return ResponseEntity.ok(
                agentWorkloadService.getAgentWorkloads());
    }

    // Automatically assign a ticket to the least busy eligible agent.
    @PostMapping("/tickets/{ticketId}/auto-assign")
    public ResponseEntity<TicketResponse> autoAssignTicket(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        Ticket ticket = autoAssignmentService.assignToLeastBusyAgent(
                ticketId,
                authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }
}