
package com.qsp.resolvehub.dto.response;

public record AgentDashboardResponse(
        long totalAssigned,
        long inProgress,
        long resolved,
        long lowPriority,
        long mediumPriority,
        long highPriority,
        long criticalPriority
) {
}
