
package com.qsp.resolvehub.repository;

import com.qsp.resolvehub.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository
        extends JpaRepository<TicketHistory, Integer> {

    List<TicketHistory> findByTicket_IdOrderByChangedAtDesc(
            Integer ticketId);
}
