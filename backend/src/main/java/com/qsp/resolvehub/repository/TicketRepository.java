package com.qsp.resolvehub.repository;

import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketPriority;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface TicketRepository
        extends JpaRepository<Ticket, Integer>,
        JpaSpecificationExecutor<Ticket> {

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

    // Ticket counts
    long countByAssignedToAndStatus(User assignedTo, TicketStatus status);

    long countByAssignedToAndPriority(User assignedTo, TicketPriority priority);

    long countByStatus(TicketStatus status);

    long countByAssignedToIsNotNull();

    long countByAssignedToIsNull();

    long countByAssignedToAndStatusIn(
            User assignedTo,
            Collection<TicketStatus> statuses
    );

    // Admin filtering
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    // Employee ticket queries
    Page<Ticket> findByRequester(User requester, Pageable pageable);

    Optional<Ticket> findByIdAndRequester(Integer id, User requester);

    // Lock a ticket during a status transition
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       SELECT t FROM Ticket t
       WHERE t.id = :ticketId
       AND t.deleted = false
       """)
    Optional<Ticket> findByIdForUpdate(
            @Param("ticketId") Integer ticketId
    );



    Page<Ticket> findByTitleContainingIgnoreCase(
            String title,
            Pageable pageable
    );

    Page<Ticket> findByPriority(
            TicketPriority priority,
            Pageable pageable
    );

    long countByAssignedTo(User assignedTo);

    Optional<Ticket> findByTicketNumber(String ticketNumber);
    // Employee dashboard counts
    long countByRequester(User requester);

    long countByRequesterAndStatus(
            User requester,
            TicketStatus status
    );

    // Active ticket lookups
    Optional<Ticket> findByIdAndDeletedFalse(Integer id);

    Optional<Ticket> findByIdAndAssignedToAndDeletedFalse(
            Integer id, User assignedTo);

    Optional<Ticket> findByIdAndRequesterAndDeletedFalse(
            Integer id, User requester);

    // Active agent ticket lists
    Page<Ticket> findByAssignedToAndDeletedFalse(
            User assignedTo, Pageable pageable);

    Page<Ticket> findByAssignedToAndStatusAndDeletedFalse(
            User assignedTo, TicketStatus status, Pageable pageable);

    // Active employee ticket lists
    Page<Ticket> findByRequesterAndDeletedFalse(
            User requester, Pageable pageable);

    // Active admin ticket lists
    Page<Ticket> findByDeletedFalse(Pageable pageable);

    Page<Ticket> findByStatusAndDeletedFalse(
            TicketStatus status, Pageable pageable);

    // Active ticket search
    Page<Ticket> findByTitleContainingIgnoreCaseAndDeletedFalse(
            String title, Pageable pageable);

    Page<Ticket> findByPriorityAndDeletedFalse(
            TicketPriority priority, Pageable pageable);

    Optional<Ticket> findByTicketNumberAndDeletedFalse(
            String ticketNumber);
    long countByStatusAndDeletedFalse(TicketStatus status);

    long countByAssignedToAndStatusAndDeletedFalse(
            User assignedTo, TicketStatus status);

    long countByAssignedToAndPriorityAndDeletedFalse(
            User assignedTo, TicketPriority priority);

    long countByAssignedToAndDeletedFalse(User assignedTo);

    long countByRequesterAndDeletedFalse(User requester);

    long countByRequesterAndStatusAndDeletedFalse(
            User requester, TicketStatus status);

    long countByAssignedToIsNotNullAndDeletedFalse();

    long countByAssignedToIsNullAndDeletedFalse();

    long countByAssignedToAndStatusInAndDeletedFalse(
            User assignedTo, Collection<TicketStatus> statuses);

}
