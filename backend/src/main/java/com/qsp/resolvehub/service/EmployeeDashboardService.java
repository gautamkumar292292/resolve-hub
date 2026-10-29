package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.response.EmployeeDashboardResponse;

public interface EmployeeDashboardService {

    EmployeeDashboardResponse getDashboard(String employeeEmail);
    
}