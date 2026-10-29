
package com.qsp.resolvehub.dto.response;

import com.qsp.resolvehub.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Integer id,
        Integer ticketId,
        String userName,
        String content,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getUser().getName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
