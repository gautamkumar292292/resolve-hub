
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.response.CommentResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponse addComment(
            Integer ticketId,
            String userEmail,
            String content
    );

    Page<CommentResponse> getTicketComments(
            Integer ticketId,
            String userEmail,
            Pageable pageable
    );

    CommentResponse updateComment(
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
