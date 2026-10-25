
package com.qsp.resolvehub.security;

import com.qsp.resolvehub.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}")
            long expirationMillis) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret must not be empty"
            );
        }

        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "JWT secret must be valid Base64",
                    ex
            );
        }

        // HS256 requires a key of at least 256 bits (32 bytes).
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must decode to at least 32 bytes"
            );
        }

        if (expirationMillis <= 0) {
            throw new IllegalArgumentException(
                    "JWT expiration must be greater than zero"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMillis = expirationMillis;
    }

    /**
     * Generates a JWT for a user.
     */
    public String generateToken(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User must not be null"
            );
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRoles().name());
        claims.put("tokenVersion", user.getTokenVersion());

        Date issuedAt = new Date();
        Date expiration = new Date(
                issuedAt.getTime() + expirationMillis
        );

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the username/email from a JWT.
     * Parsing also verifies the token signature and expiration.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a JWT.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts an individual claim.
     */
    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver) {

        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    /**
     * Parses a signed JWT and validates its signature and expiration.
     */
    private Claims extractAllClaims(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT token must not be empty"
            );
        }

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates a JWT against the current database user.
     */
    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        if (userDetails == null) {
            return false;
        }

        try {
            Claims claims = extractAllClaims(token);

            String username = claims.getSubject();
            Date expiration = claims.getExpiration();
            Object versionClaim = claims.get("tokenVersion");

            if (!(userDetails instanceof User user)) {
                log.warn(
                        "JWT validation failed: unexpected UserDetails type"
                );
                return false;
            }

            if (!(versionClaim instanceof Number version)) {
                log.debug(
                        "JWT validation failed: missing tokenVersion"
                );
                return false;
            }

            boolean usernameMatches =
                    username != null
                            && username.equalsIgnoreCase(user.getUsername());

            boolean notExpired =
                    expiration != null
                            && expiration.after(new Date());

            boolean accountEnabled = user.isEnabled();

            boolean versionMatches =
                    version.longValue() == user.getTokenVersion();

            return usernameMatches
                    && notExpired
                    && accountEnabled
                    && versionMatches;

        } catch (io.jsonwebtoken.JwtException
                 | IllegalArgumentException ex) {

            // Do not log the JWT or other sensitive token data.
            log.debug(
                    "JWT validation failed: {}",
                    ex.getClass().getSimpleName()
            );

            return false;
        }
    }

    /**
     * Returns the configured token lifetime in seconds.
     */
    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }
}
