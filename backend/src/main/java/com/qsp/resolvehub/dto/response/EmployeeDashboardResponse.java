package com.qsp.resolvehub.dto.response;

public record EmployeeDashboardResponse(
        long totalTickets,
        long openTickets,
        long assignedTickets,
        long inProgressTickets,
        long resolvedTickets,
        long closedTickets,
        long reopenedTickets
) {
}