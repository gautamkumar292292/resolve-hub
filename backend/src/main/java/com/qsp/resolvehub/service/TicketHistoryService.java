
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketHistory;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.repository.TicketHistoryRepository;
import com.qsp.resolvehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketHistoryService {

    private final TicketHistoryRepository ticketHistoryRepository;
    private final UserRepository userRepository;

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
}
