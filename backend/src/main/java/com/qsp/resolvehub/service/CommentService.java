
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    Comment addComment(
            Integer ticketId,
            String userEmail,
            String content
    );

    Page<Comment> getTicketComments(
            Integer ticketId,
            String userEmail,
            Pageable pageable
    );

    Comment updateComment(
            Integer ticketId,
            Integer commentId,
            String userEmail,
            String content
    );

    void deleteComment(
            Integer ticketId,
            Integer commentId,
            String userEmail
    );
}
