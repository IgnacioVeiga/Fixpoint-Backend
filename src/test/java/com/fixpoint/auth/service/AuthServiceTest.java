package com.fixpoint.auth.service;

import com.fixpoint.auth.domain.UserRole;
import com.fixpoint.auth.dto.AuthTokenResponse;
import com.fixpoint.auth.dto.LoginRequest;
import com.fixpoint.auth.dto.RegisterRequest;
import com.fixpoint.auth.entity.AppUser;
import com.fixpoint.auth.repository.AppUserRepository;
import com.fixpoint.auth.security.AppUserPrincipal;
import com.fixpoint.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldReturnTokenForValidCredentials() {
        LoginRequest request = new LoginRequest("Alice", "secret");
        AppUserPrincipal principal = new AppUserPrincipal(1L, "alice", "hash", UserRole.ADMIN, true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("jwt-token");
        when(jwtService.computeExpirationInstant()).thenReturn(Instant.parse("2026-02-14T12:00:00Z"));

        AuthTokenResponse response = authService.login(request);

        assertEquals("Bearer", response.tokenType());
        assertEquals("jwt-token", response.accessToken());
        assertEquals(OffsetDateTime.ofInstant(Instant.parse("2026-02-14T12:00:00Z"), ZoneOffset.UTC), response.expiresAt());
        assertEquals("alice", response.username());
        assertEquals("ADMIN", response.role());
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(new LoginRequest("alice", "wrong"))
        );

        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void registerShouldCreateTechUserByDefault() {
        RegisterRequest request = new RegisterRequest("NewUser", "safe-pass-2026");
        when(appUserRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("safe-pass-2026")).thenReturn("hashed-password");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });
        when(jwtService.generateToken(any(AppUserPrincipal.class))).thenReturn("new-token");
        when(jwtService.computeExpirationInstant()).thenReturn(Instant.parse("2026-02-14T13:00:00Z"));

        AuthTokenResponse response = authService.registerForDev(request);

        assertEquals("newuser", response.username());
        assertEquals("TECH", response.role());
        assertEquals("new-token", response.accessToken());
        verify(passwordEncoder).encode("safe-pass-2026");
    }

    @Test
    void registerShouldRejectDuplicatedUsername() {
        when(appUserRepository.existsByUsername("duplicated")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerForDev(new RegisterRequest("duplicated", "safe-pass-2026"))
        );

        assertEquals("Username is already in use", ex.getMessage());
    }
}
