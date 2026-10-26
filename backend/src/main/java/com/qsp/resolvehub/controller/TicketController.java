
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.request.TicketRequest;
import com.qsp.resolvehub.dto.response.TicketResponse;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketPriority;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.service.TicketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // EMPLOYEE: Create a ticket.
    @PostMapping("/tickets")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketRequest request,
            Authentication authentication) {

        Ticket ticket = ticketService.createTicket(
                request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketResponse.from(ticket));
    }

    // EMPLOYEE: View own tickets.
    @GetMapping("/tickets/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            Authentication authentication,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt")
            Pageable pageable) {

        Page<TicketResponse> tickets =
                ticketService.getMyEmployeeTicketResponses(
                        authentication.getName(), pageable);

        return ResponseEntity.ok(tickets);
    }

    // ADMIN / AGENT: Search and filter tickets.

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Page<TicketResponse>> getAllTickets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            Authentication authentication) {

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (size < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be greater than 0"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"))
        );
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");

            String property = parts[0].trim();

            Set<String> allowedSortFields = Set.of(
                    "id",
                    "createdAt",
                    "updatedAt",
                    "title",
                    "priority",
                    "status"
            );

            if (!allowedSortFields.contains(property)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid sort field: " + property
                );
            }

            Sort.Direction direction;

            try {
                direction = parts.length > 1
                        ? Sort.Direction.fromString(parts[1].trim())
                        : Sort.Direction.ASC;
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid sort direction. Use 'asc' or 'desc'."
                );
            }

            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(new Sort.Order(direction, property))
            );
        }
        return ResponseEntity.ok(
                ticketService.searchTickets(
                        search,
                        status,
                        priority,
                        categoryId,
                        assignedTo,
                        pageable,
                        authentication.getName()
                )
        );
    }


    // ADMIN: Manually assign a ticket to an agent.
    // POST /api/tickets/{ticketId}/assign?agentId=5
    @PostMapping("/tickets/{ticketId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Integer ticketId,
            @RequestParam Integer agentId,
            @AuthenticationPrincipal User admin) {

        TicketResponse response = ticketService.assignTicketResponse(
                ticketId,
                agentId,
                admin.getEmail()
        );

        return ResponseEntity.ok(response);
    }

    // ADMIN: Unassign a ticket.
    // POST /api/tickets/{ticketId}/unassign
    @PostMapping("/tickets/{ticketId}/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> unassignTicket(
            @PathVariable Integer ticketId,
            @AuthenticationPrincipal User admin) {

        TicketResponse response = ticketService.unassignTicket(
                ticketId,
                admin.getEmail()
        );

        return ResponseEntity.ok(response);
    }

    // AUTHENTICATED USER: Get a ticket they are authorized to access.
    @GetMapping("/tickets/{ticketId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        return ResponseEntity.ok(
                ticketService.getTicketResponseById(
                        ticketId, authentication.getName()
                )
        );
    }

    // EMPLOYEE: Update own OPEN ticket.
    @PutMapping("/tickets/{ticketId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TicketResponse> updateMyTicket(
            @PathVariable Integer ticketId,
            @Valid @RequestBody TicketRequest request,
            Authentication authentication) {

        Ticket ticket = ticketService.updateMyTicket(
                ticketId, request, authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // EMPLOYEE: Delete own eligible ticket.
    @DeleteMapping("/tickets/{ticketId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Void> deleteMyTicket(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        ticketService.deleteMyTicket(
                ticketId, authentication.getName());

        return ResponseEntity.noContent().build();
    }

    // ADMIN: Update ticket status.
    @PutMapping("/tickets/{ticketId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Integer ticketId,
            @RequestParam TicketStatus status,
            Authentication authentication) {

        Ticket updatedTicket = ticketService.updateTicketStatus(
                ticketId,
                status,
                authentication.getName()
        );

        return ResponseEntity.ok(TicketResponse.from(updatedTicket));
    }

    // EMPLOYEE: Close own RESOLVED ticket.
    // Required route: POST /api/tickets/{ticketId}/close
// EMPLOYEE / ADMIN: Close a resolved ticket.
    @PostMapping("/tickets/{ticketId}/close")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<TicketResponse> closeMyTicket(
            @PathVariable Integer ticketId,
            Authentication authentication) {
        TicketResponse response =
                ticketService.closeMyTicketResponse(
                        ticketId,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    // EMPLOYEE: Reopen own CLOSED ticket.
    // Required route: POST /api/tickets/{ticketId}/reopen
    @PostMapping("/tickets/{ticketId}/reopen")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TicketResponse> reopenMyTicket(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        Ticket ticket = ticketService.reopenMyTicket(
                ticketId, authentication.getName());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // EMPLOYEE: Get own ticket by ID.
    @GetMapping("/employee/tickets/{ticketId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TicketResponse> getMyEmployeeTicketById(
            @PathVariable Integer ticketId,
            Authentication authentication) {

        TicketResponse response =
                ticketService.getMyEmployeeTicketResponseById(
                        ticketId, authentication.getName());

        return ResponseEntity.ok(response);
    }

    // ADMIN: Filter tickets by status.
    @GetMapping("/tickets/filter/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TicketResponse>> getTicketsByStatus(
            @RequestParam TicketStatus status,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TicketResponse> result =
                ticketService.getTicketResponsesByStatus(
                        status, pageable
                );

        return ResponseEntity.ok(result);
    }


    // ADMIN: Search tickets by title.
    @GetMapping("/tickets/search/title")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TicketResponse>> searchTicketsByTitle(
            @RequestParam String title,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TicketResponse> response =
                ticketService.searchTicketResponsesByTitle(
                        title,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    // ADMIN: Filter tickets by priority.
    @GetMapping("/tickets/filter/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TicketResponse>> getTicketsByPriority(
            @RequestParam TicketPriority priority,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TicketResponse> result =
                ticketService.getTicketResponsesByPriority(
                        priority, pageable
                );

        return ResponseEntity.ok(result);
    }

    // ADMIN: Count tickets assigned to an agent.
    @GetMapping("/agents/{agentId}/tickets/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countTicketsAssignedTo(
            @PathVariable Integer agentId) {

        return ResponseEntity.ok(
                ticketService.countTicketsAssignedTo(agentId));
    }

    // ADMIN: Find a ticket by ticket number.
    @GetMapping("/tickets/number/{ticketNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> getTicketByTicketNumber(
            @PathVariable String ticketNumber) {

        TicketResponse response =
                ticketService.getTicketResponseByTicketNumber(ticketNumber);

        return ResponseEntity.ok(response);
    }

    // ADMIN: Filter tickets by category.
    @GetMapping("/tickets/filter/category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TicketResponse>> getTicketsByCategory(
            @RequestParam Integer categoryId,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TicketResponse> result =
                ticketService.getTicketResponsesByCategory(
                        categoryId, pageable
                );

        return ResponseEntity.ok(result);
    }
}
