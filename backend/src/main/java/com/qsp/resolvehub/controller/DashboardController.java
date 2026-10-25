
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.response.AdminDashboardResponse;
import com.qsp.resolvehub.dto.response.AgentDashboardResponse;
import com.qsp.resolvehub.dto.response.EmployeeDashboardResponse;
import com.qsp.resolvehub.service.AdminDashboardService;
import com.qsp.resolvehub.service.AgentDashboardService;
import com.qsp.resolvehub.service.EmployeeDashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AgentDashboardService agentDashboardService;
    private final EmployeeDashboardService employeeDashboardService;

    // ADMIN: Get admin dashboard statistics.
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(
                adminDashboardService.getDashboard()
        );
    }

    // AGENT: Get the authenticated agent's dashboard.
    @GetMapping("/agent")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<AgentDashboardResponse> getAgentDashboard(
            Authentication authentication) {

        return ResponseEntity.ok(
                agentDashboardService.getDashboard(
                        authentication.getName()
                )
        );
    }

    // EMPLOYEE: Get the authenticated employee's dashboard.
    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(
            Authentication authentication) {

        return ResponseEntity.ok(
                employeeDashboardService.getDashboard(
                        authentication.getName()
                )
        );
    }
}
