
package com.qsp.resolvehub.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        // No Bearer token: let Spring Security handle
        // whether authentication is required.
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isEmpty()) {
            rejectRequest(response, "Empty Bearer token");
            return;
        }

        try {
            String email = jwtService.extractUsername(token);

            if (email == null || email.isBlank()) {
                rejectRequest(response, "Token has no username");
                return;
            }

            // Avoid replacing an existing authentication.
            if (SecurityContextHolder.getContext()
                    .getAuthentication() != null) {

                filterChain.doFilter(request, response);
                return;
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            // Validate signature, expiration and user details
            // according to JwtService implementation.
            if (!jwtService.isTokenValid(token, userDetails)) {
                rejectRequest(response, "Invalid or expired token");
                return;
            }

            // Reject disabled/inactive accounts.
            if (!userDetails.isEnabled()) {
                rejectRequest(response, "User account is disabled");
                return;
            }

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

            log.debug(
                    "JWT authenticated request: URI={}, authorities={}",
                    request.getRequestURI(),
                    authentication.getAuthorities()
            );

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException ex) {

            log.warn(
                    "JWT validation failed for URI={}: {}",
                    request.getRequestURI(),
                    ex.getClass().getSimpleName()
            );

            rejectRequest(response, "Invalid or expired token");

        } catch (UsernameNotFoundException ex) {

            log.warn(
                    "JWT user not found for URI={}",
                    request.getRequestURI()
            );

            rejectRequest(response, "User no longer exists");
        }
    }

    private void rejectRequest(
            HttpServletResponse response,
            String reason
    ) throws IOException {

        SecurityContextHolder.clearContext();

        log.debug("Rejecting JWT request: {}", reason);

        if (!response.isCommitted()) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired authentication token"
            );
        }
    }
}
