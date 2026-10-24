
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

        long assigned = ticketRepository.countByAssignedToAndStatus(
                agent, TicketStatus.ASSIGNED);

        long inProgress = ticketRepository.countByAssignedToAndStatus(
                agent, TicketStatus.IN_PROGRESS);

        long resolved = ticketRepository.countByAssignedToAndStatus(
                agent, TicketStatus.RESOLVED);

        long low = ticketRepository.countByAssignedToAndPriority(
                agent, TicketPriority.LOW);

        long medium = ticketRepository.countByAssignedToAndPriority(
                agent, TicketPriority.MEDIUM);

        long high = ticketRepository.countByAssignedToAndPriority(
                agent, TicketPriority.HIGH);

        long critical = ticketRepository.countByAssignedToAndPriority(
                agent, TicketPriority.CRITICAL);

        return new AgentDashboardResponse(
                assigned + inProgress + resolved,
                inProgress,
                resolved,
                low,
                medium,
                high,
                critical
        );

    }
}
