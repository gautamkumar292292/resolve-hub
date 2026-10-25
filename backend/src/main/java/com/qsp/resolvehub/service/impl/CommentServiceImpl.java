
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

    private User getActiveUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (!user.isAccount_status()) {
            throw new AccessDeniedException("Account is inactive");
        }

        return user;
    }

    private Ticket getAccessibleTicket(Integer ticketId, User user) {
        return switch (user.getRole()) {
            case ADMIN -> ticketRepository.findById(ticketId)
                    .orElseThrow(() ->
                            new TicketNotFoundException("Ticket not found"));

            case EMPLOYEE -> ticketRepository
                    .findByIdAndRequester(ticketId, user)
                    .orElseThrow(() ->
                            new TicketNotFoundException(
                                    "Ticket not found or does not belong to you"));

            case AGENT -> ticketRepository
                    .findByIdAndAssignedTo(ticketId, user)
                    .orElseThrow(() ->
                            new TicketNotFoundException(
                                    "Ticket not found or not assigned to you"));
        };
    }

    private String validateContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException(
                    "Comment cannot be empty");
        }

        String trimmed = content.trim();

        if (trimmed.length() > 2000) {
            throw new IllegalArgumentException(
                    "Comment cannot exceed 2000 characters");
        }

        return trimmed;
    }

    @Override
    public Comment addComment(
            Integer ticketId,
            String userEmail,
            String content) {

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

        User user = getActiveUser(userEmail);
        Ticket ticket = getAccessibleTicket(ticketId, user);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Comment not found"));

        if (!comment.getTicket().getId().equals(ticket.getId())) {
            throw new TicketNotFoundException(
                    "Comment does not belong to this ticket");
        }

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "Only the comment author can edit it");
        }

        comment.setContent(validateContent(content));

        return commentRepository.save(comment);
    }

    @Override
    public void deleteComment(
            Integer ticketId,
            Integer commentId,
            String userEmail) {

        User user = getActiveUser(userEmail);
        Ticket ticket = getAccessibleTicket(ticketId, user);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Comment not found"));

        if (!comment.getTicket().getId().equals(ticket.getId())) {
            throw new TicketNotFoundException(
                    "Comment does not belong to this ticket");
        }

        boolean isAuthor =
                comment.getUser().getId().equals(user.getId());

        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException(
                    "Only the comment author or an admin can delete it");
        }

        commentRepository.delete(comment);
    }
}
