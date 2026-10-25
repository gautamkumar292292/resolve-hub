
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.dto.response.TicketHistoryResponse;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketHistory;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.repository.TicketHistoryRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.TicketHistoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketHistoryServiceImpl implements TicketHistoryService {

    private final TicketHistoryRepository ticketHistoryRepository;
    private final UserRepository userRepository;

    // Record a status change within the caller's transaction
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TicketHistory recordStatusChange(
            Ticket ticket,
            TicketStatus oldStatus,
            TicketStatus newStatus,
            String changedByEmail) {

        User actor = userRepository.findByEmail(changedByEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Status-change actor not found"));

        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setChangedBy(actor);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);

        return ticketHistoryRepository.save(history);
    }

    // Retrieve ticket history as response DTOs
    @Override
    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getTicketHistory(
            Integer ticketId) {

        return ticketHistoryRepository
                .findHistoryByTicketId(ticketId)
                .stream()
                .map(TicketHistoryResponse::from)
                .toList();
    }
}
