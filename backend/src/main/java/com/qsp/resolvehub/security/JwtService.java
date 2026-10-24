package com.qsp.resolvehub.security;

import com.qsp.resolvehub.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

        byte[] keyBytes = Decoders.BASE64.decode(secret);

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

    // Generate a JWT for the authenticated user.
    public String generateToken(User user) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("role", user.getRoles().name());
        claims.put("tokenVersion", user.getTokenVersion());

        Date now = new Date();
        Date expiry = new Date(
                now.getTime() + expirationMillis
        );

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract the email from a JWT.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract the expiration date from a JWT.
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract a specific claim.
    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver) {

        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    // Parse and verify the JWT signature.
    private Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Validate the JWT against the current database user.
    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

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
            log.warn(
                    "JWT validation failed: tokenVersion missing or invalid"
            );
            return false;
        }

        boolean usernameMatches =
                username != null
                        && username.equalsIgnoreCase(user.getUsername());

        boolean notExpired =
                expiration != null
                        && expiration.after(new Date());

        boolean enabled = user.isEnabled();

        boolean versionMatches =
                version.intValue() == user.getTokenVersion();

        // Do not log the JWT or any sensitive token data.
        log.debug("JWT usernameMatches: {}", usernameMatches);
        log.debug("JWT notExpired: {}", notExpired);
        log.debug("JWT enabled: {}", enabled);
        log.debug("JWT tokenVersionMatches: {}", versionMatches);
        log.debug(
                "JWT tokenVersion claim: {}, database version: {}",
                version,
                user.getTokenVersion()
        );

        return usernameMatches
                && notExpired
                && enabled
                && versionMatches;
    }

    // Return token lifetime in seconds.
    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }
}