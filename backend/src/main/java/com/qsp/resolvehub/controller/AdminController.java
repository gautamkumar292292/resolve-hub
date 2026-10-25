
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.request.UpdateRoleRequest;
import com.qsp.resolvehub.dto.request.UpdateTicketStatusRequest;
import com.qsp.resolvehub.dto.request.UpdateUserRequest;
import com.qsp.resolvehub.dto.response.AdminDashboardResponse;
import com.qsp.resolvehub.dto.response.AgentWorkloadResponse;
import com.qsp.resolvehub.dto.response.TicketResponse;
import com.qsp.resolvehub.dto.response.UserResponse;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.service.AdminDashboardService;
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
    private final AdminDashboardService adminDashboardService;
    private final TicketService ticketService;
    private final AgentWorkloadService agentWorkloadService;
    private final AutoAssignmentService autoAssignmentService;

    // Public endpoint: Get all admins without authentication.
    // SecurityConfig must explicitly permit this route.
    @GetMapping("/public/admins")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<UserResponse>> getAllAdmins() {
        return ResponseEntity.ok(userService.getAllAdmins());
    }

    // Get the currently authenticated admin.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentAdmin(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // Admin user management.
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                userService.adminUpdateUser(id, request));
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(
                userService.updateUserRole(id, request));
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                userService.setUserStatus(id, true));
    }

    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                userService.setUserStatus(id, false));
    }

    // Admin dashboard remains admin-only.
    // This mapping is handled separately below using an explicit route.
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(
                adminDashboardService.getDashboard());
    }

    // List tickets.
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

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable Integer ticketId) {
        return ResponseEntity.ok(
                TicketResponse.from(
                        ticketService.getTicketById(ticketId)));
    }

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

    // Agent workload.
    @GetMapping("/agents/workload")
    public ResponseEntity<List<AgentWorkloadResponse>> getAgentWorkloads() {
        return ResponseEntity.ok(
                agentWorkloadService.getAgentWorkloads());
    }

    // Automatic assignment.
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
