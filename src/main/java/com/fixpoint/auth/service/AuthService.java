package com.fixpoint.auth.service;

import com.fixpoint.auth.domain.UserRole;
import com.fixpoint.auth.dto.AuthTokenResponse;
import com.fixpoint.auth.dto.LoginRequest;
import com.fixpoint.auth.dto.RegisterRequest;
import com.fixpoint.auth.entity.AppUser;
import com.fixpoint.auth.entity.RefreshSession;
import com.fixpoint.auth.exception.AuthenticationFailedException;
import com.fixpoint.auth.repository.AppUserRepository;
import com.fixpoint.auth.repository.RefreshSessionRepository;
import com.fixpoint.auth.security.AppUserPrincipal;
import com.fixpoint.auth.security.JwtService;
import com.fixpoint.auth.security.RefreshTokenCookieService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final ZoneOffset STORAGE_ZONE = ZoneOffset.UTC;
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password";
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Invalid or expired session";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @Value("${security.auth.refresh.expiration-seconds:43200}")
    private long refreshExpirationSeconds;

    @Value("${security.auth.refresh.remember-expiration-seconds:2592000}")
    private long refreshRememberExpirationSeconds;

    @PostConstruct
    void validateAuthConfiguration() {
        if (refreshExpirationSeconds <= 0) {
            throw new IllegalStateException("security.auth.refresh.expiration-seconds must be positive");
        }
        if (refreshRememberExpirationSeconds <= 0) {
            throw new IllegalStateException("security.auth.refresh.remember-expiration-seconds must be positive");
        }
        if (refreshRememberExpirationSeconds < refreshExpirationSeconds) {
            throw new IllegalStateException(
                    "security.auth.refresh.remember-expiration-seconds must be >= security.auth.refresh.expiration-seconds"
            );
        }
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request, HttpServletResponse response) {
        String normalizedUsername = normalizeUsername(request.username());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedUsername, request.password())
            );
        } catch (AuthenticationException ex) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS_MESSAGE);
        }

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
        IssuedRefreshToken issuedRefreshToken = createRefreshSession(principal.getId(), rememberMe);
        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshTokenCookieService.buildRefreshCookieHeader(
                        issuedRefreshToken.rawToken(),
                        issuedRefreshToken.maxAgeSeconds()
                ));

        return buildAccessTokenResponse(principal);
    }

    @Transactional
    public AuthTokenResponse registerForDev(RegisterRequest request, HttpServletResponse response) {
        String normalizedUsername = normalizeUsername(request.username());

        if (appUserRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username is already in use");
        }

        AppUser user = AppUser.builder()
                .username(normalizedUsername)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.TECH)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        AppUser savedUser = appUserRepository.save(user);

        IssuedRefreshToken issuedRefreshToken = createRefreshSession(savedUser.getId(), false);
        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshTokenCookieService.buildRefreshCookieHeader(
                        issuedRefreshToken.rawToken(),
                        issuedRefreshToken.maxAgeSeconds()
                ));

        return buildAccessTokenResponse(AppUserPrincipal.fromEntity(savedUser));
    }

    @Transactional
    public AuthTokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = refreshTokenCookieService.extractRefreshToken(request)
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE));
        String tokenHash = hashRefreshToken(rawRefreshToken);
        RefreshSession currentSession = refreshSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> invalidRefreshToken(response));

        LocalDateTime now = nowUtc();
        if (currentSession.getRevokedAt() != null || !currentSession.getExpiresAt().isAfter(now)) {
            markSessionRevokedIfNeeded(currentSession, now);
            throw invalidRefreshToken(response);
        }

        AppUser user = appUserRepository.findById(currentSession.getUserId())
                .orElseThrow(() -> invalidRefreshToken(response));
        if (!user.isEnabled()) {
            markSessionRevokedIfNeeded(currentSession, now);
            throw invalidRefreshToken(response);
        }

        IssuedRefreshToken issuedRefreshToken = rotateRefreshSession(currentSession, now);
        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshTokenCookieService.buildRefreshCookieHeader(
                        issuedRefreshToken.rawToken(),
                        issuedRefreshToken.maxAgeSeconds()
                ));

        return buildAccessTokenResponse(AppUserPrincipal.fromEntity(user));
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenCookieService.extractRefreshToken(request)
                .map(this::hashRefreshToken)
                .flatMap(refreshSessionRepository::findByTokenHash)
                .ifPresent(session -> {
                    if (session.getRevokedAt() == null) {
                        LocalDateTime now = nowUtc();
                        session.setRevokedAt(now);
                        session.setLastUsedAt(now);
                        refreshSessionRepository.save(session);
                    }
                });

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.buildClearCookieHeader());
    }

    private AuthenticationFailedException invalidRefreshToken(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.buildClearCookieHeader());
        return new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE);
    }

    private AuthTokenResponse buildAccessTokenResponse(AppUserPrincipal principal) {
        String token = jwtService.generateToken(principal);
        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(jwtService.computeExpirationInstant(), ZoneOffset.UTC);

        return new AuthTokenResponse(
                "Bearer",
                token,
                expiresAt,
                principal.getUsername(),
                principal.getRole().name()
        );
    }

    private IssuedRefreshToken createRefreshSession(Long userId, boolean rememberMe) {
        long ttlSeconds = rememberMe ? refreshRememberExpirationSeconds : refreshExpirationSeconds;
        LocalDateTime now = nowUtc();
        LocalDateTime expiresAt = now.plusSeconds(ttlSeconds);
        return saveRefreshSession(userId, rememberMe, now, expiresAt, ttlSeconds);
    }

    private IssuedRefreshToken rotateRefreshSession(RefreshSession currentSession, LocalDateTime now) {
        markSessionRevokedIfNeeded(currentSession, now);

        long maxAgeSeconds = ChronoUnit.SECONDS.between(now, currentSession.getExpiresAt());
        if (maxAgeSeconds <= 0) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        return saveRefreshSession(
                currentSession.getUserId(),
                currentSession.isRememberMe(),
                now,
                currentSession.getExpiresAt(),
                maxAgeSeconds
        );
    }

    private IssuedRefreshToken saveRefreshSession(
            Long userId,
            boolean rememberMe,
            LocalDateTime now,
            LocalDateTime expiresAt,
            long maxAgeSeconds
    ) {
        String rawToken = generateRefreshToken();
        RefreshSession newSession = RefreshSession.builder()
                .userId(userId)
                .tokenHash(hashRefreshToken(rawToken))
                .rememberMe(rememberMe)
                .issuedAt(now)
                .lastUsedAt(now)
                .expiresAt(expiresAt)
                .build();
        refreshSessionRepository.save(newSession);
        return new IssuedRefreshToken(rawToken, maxAgeSeconds);
    }

    private void markSessionRevokedIfNeeded(RefreshSession session, LocalDateTime now) {
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(now);
            session.setLastUsedAt(now);
            refreshSessionRepository.save(session);
        }
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(Instant.now(), STORAGE_ZONE);
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashRefreshToken(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawRefreshToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private String normalizeUsername(String rawUsername) {
        return Objects.requireNonNullElse(rawUsername, "").trim().toLowerCase(Locale.ROOT);
    }

    private record IssuedRefreshToken(String rawToken, long maxAgeSeconds) {
    }
}
