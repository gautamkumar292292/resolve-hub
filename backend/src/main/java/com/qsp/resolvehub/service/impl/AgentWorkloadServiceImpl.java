
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.dto.response.AgentWorkloadResponse;
import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.repository.TicketRepository;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.service.AgentWorkloadService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentWorkloadServiceImpl implements AgentWorkloadService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    @Override
    public List<AgentWorkloadResponse> getAgentWorkloads() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == Role.AGENT)
                .filter(User::isAccount_status)
                .map(this::toResponse)
                .toList();
    }

    private AgentWorkloadResponse toResponse(User agent) {
        long activeTickets =
                ticketRepository.countByAssignedToAndStatusInAndDeletedFalse(
                        agent,
                        List.of(
                                TicketStatus.ASSIGNED,
                                TicketStatus.IN_PROGRESS
                        )
                );

        return new AgentWorkloadResponse(
                agent.getId(),
                agent.getName(),
                agent.getEmail(),
                activeTickets
        );
    }
}
