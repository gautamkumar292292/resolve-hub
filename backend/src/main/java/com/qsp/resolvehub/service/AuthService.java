package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.request.LoginRequest;
import com.qsp.resolvehub.dto.request.RegisterRequest;
import com.qsp.resolvehub.dto.response.AuthResponse;
import com.qsp.resolvehub.dto.response.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String name);
}