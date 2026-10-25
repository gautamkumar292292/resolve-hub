
package com.qsp.resolvehub.repository;

import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketPriority;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    // Agent ticket queries
    Page<Ticket> findByAssignedTo(User assignedTo, Pageable pageable);

    Optional<Ticket> findByIdAndAssignedTo(Integer id, User assignedTo);

    Page<Ticket> findByAssignedToAndStatus(
            User assignedTo,
            TicketStatus status,
            Pageable pageable
    );

    // Ticket validation and lookup
    boolean existsByTicketNumber(String ticketNumber);

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    // Ticket counts
    long countByAssignedToAndStatus(User assignedTo, TicketStatus status);

    long countByAssignedToAndPriority(User assignedTo, TicketPriority priority);

    long countByStatus(TicketStatus status);

    long countByAssignedToIsNotNull();

    long countByAssignedToIsNull();

    long countByAssignedTo(User assignedTo);

    long countByAssignedToAndStatusIn(
            User assignedTo,
            Collection<TicketStatus> statuses
    );

    // Admin filtering
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Page<Ticket> findByTitleContainingIgnoreCase(
            String title,
            Pageable pageable
    );

    Page<Ticket> findByPriority(TicketPriority priority, Pageable pageable);

    // Employee ticket queries
    Page<Ticket> findByRequester(User requester, Pageable pageable);

    Optional<Ticket> findByIdAndRequester(Integer id, User requester);

    // Lock a ticket during a status transition
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :ticketId")
    Optional<Ticket> findByIdForUpdate(
            @Param("ticketId") Integer ticketId
    );
}
