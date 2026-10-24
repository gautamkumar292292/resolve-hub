
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.response.AgentDashboardResponse;

public interface AgentDashboardService {

    AgentDashboardResponse getDashboard(String agentEmail);
}
