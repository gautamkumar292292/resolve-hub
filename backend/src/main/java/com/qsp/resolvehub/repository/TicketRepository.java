
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

    // Get all tickets assigned to a particular agent
    Page<Ticket> findByAssignedTo(User assignedTo, Pageable pageable);

    // Get one ticket only if it belongs to the specified agent
    Optional<Ticket> findByIdAndAssignedTo(Integer id, User assignedTo);

    // Get assigned tickets filtered by status
    Page<Ticket> findByAssignedToAndStatus(
            User assignedTo,
            TicketStatus status,
            Pageable pageable
    );

    // Check whether a ticket number already exists
    boolean existsByTicketNumber(String ticketNumber);



    long countByAssignedToAndStatus(
            com.qsp.resolvehub.entity.User assignedTo,
            com.qsp.resolvehub.entity.TicketStatus status
    );


    long countByAssignedToAndPriority(
            com.qsp.resolvehub.entity.User assignedTo,
            com.qsp.resolvehub.entity.TicketPriority priority
    );

    long countByStatus(TicketStatus status);

    long countByAssignedToIsNotNull();

    long countByAssignedToIsNull();

    Optional<Ticket> findById(Integer id);

    Page<Ticket> findByStatus(
            TicketStatus status,
            Pageable pageable
    );

    long countByAssignedTo(User assignedTo);

    Page<Ticket> findByTitleContainingIgnoreCase(
            String title,
            Pageable pageable
    );

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    Page<Ticket> findByPriority(
            TicketPriority priority,
            Pageable pageable
    );

    long countByAssignedToAndStatusIn(
            User assignedTo,
            Collection<TicketStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :ticketId")
    Optional<Ticket> findByIdForUpdate(
            @Param("ticketId") Integer ticketId
    );
    Optional<Ticket> findByIdAndRequester(
            Integer id,
            User requester
    );


}
