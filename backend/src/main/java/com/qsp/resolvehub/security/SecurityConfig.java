package com.qsp.resolvehub.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.sendError(
                                            HttpServletResponse.SC_UNAUTHORIZED,
                                            "Unauthorized: Authentication required"
                                    );
                                }
                        )
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {
                                    response.sendError(
                                            HttpServletResponse.SC_FORBIDDEN,
                                            "Forbidden: Access denied"
                                    );
                                }
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public authentication endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/admin/public/admins"
                        ).permitAll()

                        // Authenticated users of any role
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/logout",
                                "/api/categories/**"
                        ).authenticated()

                        // Employee can update their own profile
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/me"
                        ).hasRole("EMPLOYEE")

                        // Admin-only user management
                        .requestMatchers(
                                "/api/users/**"
                        ).hasRole("ADMIN")

                        // Role-specific dashboards
                        .requestMatchers("/api/dashboard/admin")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/dashboard/agent")
                        .hasRole("AGENT")

                        .requestMatchers("/api/dashboard/employee")
                        .hasRole("EMPLOYEE")

                        // Admin-specific routes
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Ticket endpoint permissions are also enforced
                        // through controller-level @PreAuthorize rules
                        .requestMatchers("/api/tickets/**")
                        .authenticated()

                        // Existing role-specific routes
                        .requestMatchers("/api/employee/**")
                        .hasRole("EMPLOYEE")

                        .requestMatchers("/api/agent/**")
                        .hasRole("AGENT")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET", "POST", "PUT",
                        "PATCH", "DELETE", "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
