
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.entity.Comment;
import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.exception.TicketNotFoundException;
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

    private User getAgent(String email) {
        User agent = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AccessDeniedException("Agent not found"));

        if (agent.getRole() != Role.AGENT) {
            throw new AccessDeniedException(
                    "Only agents can access this resource");
        }

        return agent;
    }

    private Ticket getAssignedTicket(Integer ticketId, User agent) {
        return ticketRepository
                .findByIdAndAssignedTo(ticketId, agent)
                .orElseThrow(() ->
                        new TicketNotFoundException(
                                "Ticket not found or not assigned to you"));
    }

    @Override
    public Comment addComment(
            Integer ticketId,
            String agentEmail,
            String content) {

        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException(
                    "Comment cannot be empty");
        }

        String trimmedContent = content.trim();

        if (trimmedContent.length() > 2000) {
            throw new IllegalArgumentException(
                    "Comment cannot exceed 2000 characters");
        }

        User agent = getAgent(agentEmail);
        Ticket ticket = getAssignedTicket(ticketId, agent);

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(agent);
        comment.setContent(trimmedContent);

        return commentRepository.save(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> getTicketComments(
            Integer ticketId,
            String agentEmail,
            Pageable pageable) {

        User agent = getAgent(agentEmail);
        Ticket ticket = getAssignedTicket(ticketId, agent);

        return commentRepository
                .findByTicketOrderByCreatedAtDesc(ticket, pageable);
    }
}
