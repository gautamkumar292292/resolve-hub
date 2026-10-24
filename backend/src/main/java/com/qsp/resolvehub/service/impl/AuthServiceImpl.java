package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.dto.request.LoginRequest;
import com.qsp.resolvehub.dto.request.RegisterRequest;
import com.qsp.resolvehub.dto.response.AuthResponse;
import com.qsp.resolvehub.dto.response.RegisterResponse;
import com.qsp.resolvehub.dto.response.UserResponse;
import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.User;
import com.qsp.resolvehub.repository.UserRepository;
import com.qsp.resolvehub.security.JwtService;
import com.qsp.resolvehub.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "This email is already registered"
            );
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(email);

        user.setPhone(
                request.getPhone() == null
                        || request.getPhone().isBlank()
                        ? null
                        : request.getPhone().trim()
        );

        // Store only the encoded password.
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Public registration always creates an EMPLOYEE.
        user.setRoles(Role.EMPLOYEE);
        user.setAccount_status(true);

        User savedUser = userRepository.save(user);

        log.info("User registered successfully");

        return new RegisterResponse(
                "Registration successful",
                UserResponse.from(savedUser)
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        // Authenticate credentials first.
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.getPassword()
                        )
                );

        User authenticatedUser = (User) authentication.getPrincipal();

        // Increment the token version to invalidate older tokens.
        int updatedRows = userRepository.incrementTokenVersion(
                authenticatedUser.getId()
        );

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Unable to update token version"
            );
        }

        // Fetch the user again to obtain the latest database version.
        User currentUser = userRepository
                .findById(authenticatedUser.getId())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Authenticated user no longer exists"
                        )
                );

        // Generate JWT using the updated token version.
        String token = jwtService.generateToken(currentUser);

        String expiresIn = String.valueOf(
                jwtService.getExpirationSeconds()
        );

        log.info("Login successful; JWT generated");

        return new AuthResponse(token, expiresIn);
    }


    @Override
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        int updatedRows =
                userRepository.incrementTokenVersion(user.getId());

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Unable to invalidate user token");
        }
    }


    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {

        User user = userRepository
                .findByEmail(normalizeEmail(email))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        return UserResponse.from(user);
    }

}