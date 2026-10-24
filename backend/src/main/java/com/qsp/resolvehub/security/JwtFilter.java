
package com.qsp.resolvehub.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("JWT filter processing: {}", request.getRequestURI());

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            log.debug("JWT skipped: missing Bearer token");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            String email = jwtService.extractUsername(token);

            if (email == null || email.isBlank()) {
                log.warn("JWT rejected: subject is missing");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                boolean valid =
                        jwtService.isTokenValid(token, userDetails);

                log.debug("JWT validation result: {}", valid);

                if (valid) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    String authorities =
                            userDetails.getAuthorities()
                                    .stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.joining(", "));

                    log.info(
                            "JWT authenticated user: {}, authorities: {}",
                            email,
                            authorities
                    );

                } else {
                    log.warn("JWT validation failed for user: {}", email);
                    SecurityContextHolder.clearContext();
                }
            }

        } catch (JwtException
                 | IllegalArgumentException
                 | UsernameNotFoundException ex) {

            log.warn(
                    "JWT authentication failed: {}",
                    ex.getClass().getSimpleName()
            );

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
