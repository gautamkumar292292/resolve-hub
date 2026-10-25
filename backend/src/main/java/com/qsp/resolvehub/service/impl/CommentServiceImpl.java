
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.entity.Comment;
import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.exception.TicketNotFoundException;
import com.qsp.resolvehub.exception.UserNotFoundException;
import com.qsp.resolvehub.repository.CommentRepository;
import com.qsp.resolvehub.repository.TicketRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.CommentService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    private void validateIds(Integer ticketId, Integer commentId) {
        if (ticketId == null || ticketId <= 0) {
            throw new IllegalArgumentException(
                    "Ticket ID must be a positive number"
            );
        }

        if (commentId != null && commentId <= 0) {
            throw new IllegalArgumentException(
                    "Comment ID must be a positive number"
            );
        }
    }

    private User getActiveUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AccessDeniedException(
                    "Authenticated user email is required"
            );
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (!user.isAccount_status()) {
            throw new AccessDeniedException("Account is inactive");
        }

        return user;
    }

    private Ticket getAccessibleTicket(Integer ticketId, User user) {
        validateIds(ticketId, null);

        return switch (user.getRole()) {
            case ADMIN -> ticketRepository.findById(ticketId)
                    .orElseThrow(() ->
                            new TicketNotFoundException(
                                    "Ticket not found"
                            ));

            case EMPLOYEE -> ticketRepository
                    .findByIdAndRequester(ticketId, user)
                    .orElseThrow(() ->
                            new TicketNotFoundException(
                                    "Ticket not found or does not belong to you"
                            ));

            case AGENT -> ticketRepository
                    .findByIdAndAssignedTo(ticketId, user)
                    .orElseThrow(() ->
                            new TicketNotFoundException(
                                    "Ticket not found or not assigned to you"
                            ));
        };
    }

    private String validateContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException(
                    "Comment cannot be empty"
            );
        }

        String trimmed = content.trim();

        if (trimmed.length() > 2000) {
            throw new IllegalArgumentException(
                    "Comment cannot exceed 2000 characters"
            );
        }

        return trimmed;
    }

    private Comment getCommentForTicket(
            Integer ticketId,
            Integer commentId,
            Ticket ticket) {

        validateIds(ticketId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Comment not found"
                        ));

        if (comment.getTicket() == null
                || !comment.getTicket().getId().equals(ticket.getId())) {
            throw new TicketNotFoundException(
                    "Comment does not belong to this ticket"
            );
        }

        return comment;
    }

    @Override
    public Comment addComment(
            Integer ticketId,
            String userEmail,
            String content) {

        validateIds(ticketId, null);

        User user = getActiveUser(userEmail);
        Ticket ticket = getAccessibleTicket(ticketId, user);

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(user);
        comment.setContent(validateContent(content));

        return commentRepository.save(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> getTicketComments(
            Integer ticketId,
            String userEmail,
            Pageable pageable) {

        validateIds(ticketId, null);

        User user = getActiveUser(userEmail);
        Ticket ticket = getAccessibleTicket(ticketId, user);

        return commentRepository
                .findByTicketOrderByCreatedAtDesc(ticket, pageable);
    }

    @Override
    public Comment updateComment(
            Integer ticketId,
            Integer commentId,
            String userEmail,
            String content) {

        validateIds(ticketId, commentId);

        User user = getActiveUser(userEmail);
        Ticket ticket = getAccessibleTicket(ticketId, user);

        Comment comment = getCommentForTicket(
                ticketId,
                commentId,
                ticket
        );

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "Only the comment author can edit it"
            );
        }

        comment.setContent(validateContent(content));

        return commentRepository.save(comment);
    }

    @Override
    public void deleteComment(
            Integer ticketId,
            Integer commentId,
            String userEmail) {

        validateIds(ticketId, commentId);

        User user = getActiveUser(userEmail);
        Ticket ticket = getAccessibleTicket(ticketId, user);

        Comment comment = getCommentForTicket(
                ticketId,
                commentId,
                ticket
        );

        boolean isAuthor =
                comment.getUser().getId().equals(user.getId());

        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException(
                    "Only the comment author or an admin can delete it"
            );
        }

        commentRepository.delete(comment);
    }
}
