package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.request.AssignTicketRequest;
import com.qsp.resolvehub.dto.request.UpdateRoleRequest;
import com.qsp.resolvehub.dto.request.UpdateTicketStatusRequest;
import com.qsp.resolvehub.dto.request.UpdateUserRequest;
import com.qsp.resolvehub.dto.response.AdminDashboardResponse;
import com.qsp.resolvehub.dto.response.AgentWorkloadResponse;
import com.qsp.resolvehub.dto.response.TicketResponse;
import com.qsp.resolvehub.dto.response.UserResponse;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.service.*;

import com.qsp.resolvehub.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AdminDashboardService adminDashboardService;
    private final TicketService ticketService;
    private final AgentWorkloadService agentWorkloadService;
    private final AutoAssignmentService autoAssignmentService;

    // Public endpoint: Get all admins without a token
    @GetMapping("/public/admins")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<UserResponse>> getAllAdmins() {
        return ResponseEntity.ok(userService.getAllAdmins());
    }

    // Get the currently authenticated admin
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getCurrentAdmin(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // Get all users
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Get a user by ID
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // Update a user's name, email, and phone
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                userService.adminUpdateUser(id, request)
        );
    }

    // Change a user's role
    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(
                userService.updateUserRole(id, request)
        );
    }

    // Activate a user account
    @PatchMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                userService.setUserStatus(id, true)
        );
    }

    // Deactivate a user account
    @PatchMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deactivateUser(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                userService.setUserStatus(id, false)
        );
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(
                adminDashboardService.getDashboard()
        );
    }

    @PatchMapping("/tickets/{ticketId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Integer ticketId,
            @Valid @RequestBody AssignTicketRequest request) {

        return ResponseEntity.ok(
                TicketResponse.from(
                        ticketService.assignTicket(
                                ticketId,
                                request.agentId()
                        )
                )
        );
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TicketResponse>> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<TicketResponse> tickets = ticketService
                .getAllTickets(status, pageable)
                .map(TicketResponse::from);

        return ResponseEntity.ok(tickets);
    }


    @GetMapping("/tickets/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable Integer ticketId) {

        return ResponseEntity.ok(
                TicketResponse.from(
                        ticketService.getTicketById(ticketId)
                )
        );
    }

    @PatchMapping("/tickets/{ticketId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Integer ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request) {

        return ResponseEntity.ok(
                TicketResponse.from(
                        ticketService.updateTicketStatus(
                                ticketId,
                                request.status()
                        )
                )
        );
    }

    @GetMapping("/agents/workload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AgentWorkloadResponse>> getAgentWorkloads() {
        return ResponseEntity.ok(
                agentWorkloadService.getAgentWorkloads()
        );
    }

    @PostMapping("/tickets/{ticketId}/auto-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> autoAssignTicket(
            @PathVariable Integer ticketId) {

        return ResponseEntity.ok(
                TicketResponse.from(
                        autoAssignmentService.assignToLeastBusyAgent(ticketId)
                )
        );
    }
}
