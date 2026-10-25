
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.response.TicketHistoryResponse;
import com.qsp.resolvehub.service.TicketHistoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketHistoryController {

    private final TicketHistoryService ticketHistoryService;

    // ADMIN and AGENT: View ticket status history
    @GetMapping("/{ticketId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<List<TicketHistoryResponse>> getTicketHistory(
            @PathVariable Integer ticketId) {

        List<TicketHistoryResponse> history =
                ticketHistoryService.getTicketHistory(ticketId);

        return ResponseEntity.ok(history);
    }
}
