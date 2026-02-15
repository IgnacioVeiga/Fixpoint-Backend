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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyLong;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenCookieService refreshTokenCookieService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldReturnTokenForValidCredentials() {
        ReflectionTestUtils.setField(authService, "refreshExpirationSeconds", 43200L);
        ReflectionTestUtils.setField(authService, "refreshRememberExpirationSeconds", 2592000L);

        LoginRequest request = new LoginRequest("Alice", "secret", false);
        AppUserPrincipal principal = new AppUserPrincipal(1L, "alice", "hash", UserRole.ADMIN, true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("jwt-token");
        when(jwtService.computeExpirationInstant()).thenReturn(Instant.parse("2026-02-14T12:00:00Z"));
        when(refreshSessionRepository.save(any(RefreshSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenCookieService.buildRefreshCookieHeader(any(String.class), anyLong()))
                .thenReturn("fixpoint_refresh_token=token-abc; Path=/api/auth; HttpOnly");

        AuthTokenResponse tokenResponse = authService.login(request, response);

        assertEquals("Bearer", tokenResponse.tokenType());
        assertEquals("jwt-token", tokenResponse.accessToken());
        assertEquals(OffsetDateTime.ofInstant(Instant.parse("2026-02-14T12:00:00Z"), ZoneOffset.UTC), tokenResponse.expiresAt());
        assertEquals("alice", tokenResponse.username());
        assertEquals("ADMIN", tokenResponse.role());
        assertEquals("fixpoint_refresh_token=token-abc; Path=/api/auth; HttpOnly", response.getHeader("Set-Cookie"));
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        AuthenticationFailedException ex = assertThrows(
                AuthenticationFailedException.class,
                () -> authService.login(new LoginRequest("alice", "wrong", false), new MockHttpServletResponse())
        );

        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void registerShouldCreateTechUserByDefault() {
        ReflectionTestUtils.setField(authService, "refreshExpirationSeconds", 43200L);
        ReflectionTestUtils.setField(authService, "refreshRememberExpirationSeconds", 2592000L);

        RegisterRequest request = new RegisterRequest("NewUser", "safe-pass-2026");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(appUserRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("safe-pass-2026")).thenReturn("hashed-password");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });
        when(refreshSessionRepository.save(any(RefreshSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenCookieService.buildRefreshCookieHeader(any(String.class), anyLong()))
                .thenReturn("fixpoint_refresh_token=token-xyz; Path=/api/auth; HttpOnly");
        when(jwtService.generateToken(any(AppUserPrincipal.class))).thenReturn("new-token");
        when(jwtService.computeExpirationInstant()).thenReturn(Instant.parse("2026-02-14T13:00:00Z"));

        AuthTokenResponse tokenResponse = authService.registerForDev(request, response);

        assertEquals("newuser", tokenResponse.username());
        assertEquals("TECH", tokenResponse.role());
        assertEquals("new-token", tokenResponse.accessToken());
        assertEquals("fixpoint_refresh_token=token-xyz; Path=/api/auth; HttpOnly", response.getHeader("Set-Cookie"));
        verify(passwordEncoder).encode("safe-pass-2026");
    }

    @Test
    void registerShouldRejectDuplicatedUsername() {
        when(appUserRepository.existsByUsername("duplicated")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerForDev(new RegisterRequest("duplicated", "safe-pass-2026"), new MockHttpServletResponse())
        );

        assertEquals("Username is already in use", ex.getMessage());
    }

    @Test
    void refreshShouldRejectMissingCookie() {
        AuthenticationFailedException ex = assertThrows(
                AuthenticationFailedException.class,
                () -> authService.refresh(new MockHttpServletRequest(), new MockHttpServletResponse())
        );

        assertEquals("Invalid or expired session", ex.getMessage());
    }
}
