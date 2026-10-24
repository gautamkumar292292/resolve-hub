
package com.qsp.resolvehub.repository;

import com.qsp.resolvehub.entity.Comment;
import com.qsp.resolvehub.entity.Ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    // Fetch comments for a particular ticket
    Page<Comment> findByTicketOrderByCreatedAtDesc(
            Ticket ticket,
            Pageable pageable
    );
}
