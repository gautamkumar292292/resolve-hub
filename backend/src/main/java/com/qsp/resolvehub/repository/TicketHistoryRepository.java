
package com.qsp.resolvehub.repository;

import com.qsp.resolvehub.entity.TicketHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketHistoryRepository
        extends JpaRepository<TicketHistory, Integer> {

    @Query("""
        SELECT h
        FROM TicketHistory h
        JOIN FETCH h.ticket
        JOIN FETCH h.changedBy
        WHERE h.ticket.id = :ticketId
        ORDER BY h.changedAt DESC
        """)
    List<TicketHistory> findHistoryByTicketId(
            @Param("ticketId") Integer ticketId
    );
}
