
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.entity.Comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    Comment addComment(
            Integer ticketId,
            String agentEmail,
            String content
    );

    Page<Comment> getTicketComments(
            Integer ticketId,
            String agentEmail,
            Pageable pageable
    );
}
