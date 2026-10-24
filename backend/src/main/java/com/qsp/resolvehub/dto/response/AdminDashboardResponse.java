package com.qsp.resolvehub.dto.response;

public record AdminDashboardResponse(
        long totalUsers,
        long totalAgents,
        long totalEmployees,
        long totalTickets,
        long openTickets,
        long assignedTickets,
        long inProgressTickets,
        long resolvedTickets,
        long closedTickets,
        long unassignedTickets
) {
}