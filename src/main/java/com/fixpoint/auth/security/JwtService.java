package com.fixpoint.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.access-expiration-seconds:900}")
    private long jwtAccessExpirationSeconds;

    @PostConstruct
    public void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("security.jwt.secret must have at least 32 characters");
        }
        if (jwtAccessExpirationSeconds <= 0) {
            throw new IllegalStateException("security.jwt.access-expiration-seconds must be positive");
        }
    }

    public String generateToken(AppUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtAccessExpirationSeconds);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("role", principal.getRole().name())
                .claim("uid", principal.getId())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return StringUtils.hasText(username)
                && username.equals(userDetails.getUsername())
                && userDetails.isEnabled()
                && !isTokenExpired(token);
    }

    public Instant computeExpirationInstant() {
        return Instant.now().plusSeconds(jwtAccessExpirationSeconds);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
