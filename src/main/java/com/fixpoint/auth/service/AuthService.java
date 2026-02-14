package com.fixpoint.auth.service;

import com.fixpoint.auth.domain.UserRole;
import com.fixpoint.auth.dto.AuthTokenResponse;
import com.fixpoint.auth.dto.LoginRequest;
import com.fixpoint.auth.dto.RegisterRequest;
import com.fixpoint.auth.entity.AppUser;
import com.fixpoint.auth.repository.AppUserRepository;
import com.fixpoint.auth.security.AppUserPrincipal;
import com.fixpoint.auth.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthTokenResponse login(LoginRequest request) {
        String normalizedUsername = normalizeUsername(request.username());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedUsername, request.password())
            );
        } catch (AuthenticationException ex) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return buildResponse(principal);
    }

    @Transactional
    public AuthTokenResponse registerForDev(RegisterRequest request) {
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
        return buildResponse(AppUserPrincipal.fromEntity(savedUser));
    }

    private AuthTokenResponse buildResponse(AppUserPrincipal principal) {
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

    private String normalizeUsername(String rawUsername) {
        return rawUsername.trim().toLowerCase(Locale.ROOT);
    }
}
