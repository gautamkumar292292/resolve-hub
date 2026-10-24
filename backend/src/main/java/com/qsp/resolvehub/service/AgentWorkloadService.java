package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.response.AgentWorkloadResponse;
import java.util.List;

public interface AgentWorkloadService {

    List<AgentWorkloadResponse> getAgentWorkloads();
}