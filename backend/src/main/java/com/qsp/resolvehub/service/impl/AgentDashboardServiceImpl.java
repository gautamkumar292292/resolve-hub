
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.dto.response.AgentDashboardResponse;
import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.TicketPriority;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.repository.TicketRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.AgentDashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentDashboardServiceImpl implements AgentDashboardService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Override
    public AgentDashboardResponse getDashboard(String agentEmail) {

        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() ->
                        new AccessDeniedException("Agent not found"));

        if (agent.getRole() != Role.AGENT) {
            throw new AccessDeniedException(
                    "Only agents can access this resource");
        }

        // Count active tickets assigned to this agent.
        long assigned = ticketRepository
                .countByAssignedToAndStatusAndDeletedFalse(
                        agent, TicketStatus.ASSIGNED);

        long inProgress = ticketRepository
                .countByAssignedToAndStatusAndDeletedFalse(
                        agent, TicketStatus.IN_PROGRESS);

        long resolved = ticketRepository
                .countByAssignedToAndStatusAndDeletedFalse(
                        agent, TicketStatus.RESOLVED);

        // Count active tickets by priority for this agent.
        long low = ticketRepository
                .countByAssignedToAndPriorityAndDeletedFalse(
                        agent, TicketPriority.LOW);

        long medium = ticketRepository
                .countByAssignedToAndPriorityAndDeletedFalse(
                        agent, TicketPriority.MEDIUM);

        long high = ticketRepository
                .countByAssignedToAndPriorityAndDeletedFalse(
                        agent, TicketPriority.HIGH);

        long critical = ticketRepository
                .countByAssignedToAndPriorityAndDeletedFalse(
                        agent, TicketPriority.CRITICAL);

        long closed = ticketRepository
                .countByAssignedToAndStatusAndDeletedFalse(
                        agent, TicketStatus.CLOSED);

        return new AgentDashboardResponse(
                assigned + inProgress + resolved + closed,
                inProgress,
                resolved,
                low,
                medium,
                high,
                critical
        );
    }
}
