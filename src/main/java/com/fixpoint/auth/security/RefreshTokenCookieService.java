package com.fixpoint.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Service
public class RefreshTokenCookieService {

    @Value("${security.auth.refresh.cookie.name:fixpoint_refresh_token}")
    private String refreshCookieName;

    @Value("${security.auth.refresh.cookie.path:/api/v1/auth}")
    private String refreshCookiePath;

    @Value("${security.auth.refresh.cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${security.auth.refresh.cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${security.auth.refresh.cookie.domain:}")
    private String refreshCookieDomain;

    public String buildRefreshCookieHeader(String refreshToken, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path(refreshCookiePath)
                .sameSite(normalizeSameSite(refreshCookieSameSite))
                .maxAge(Duration.ofSeconds(Math.max(maxAgeSeconds, 0)));

        if (StringUtils.hasText(refreshCookieDomain)) {
            builder.domain(refreshCookieDomain.trim());
        }

        return builder.build().toString();
    }

    public String buildClearCookieHeader() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path(refreshCookiePath)
                .sameSite(normalizeSameSite(refreshCookieSameSite))
                .maxAge(Duration.ZERO);

        if (StringUtils.hasText(refreshCookieDomain)) {
            builder.domain(refreshCookieDomain.trim());
        }

        return builder.build().toString();
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null || request.getCookies().length == 0) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> refreshCookieName.equals(cookie.getName()))
                .map(cookie -> cookie.getValue() == null ? "" : cookie.getValue().trim())
                .filter(StringUtils::hasText)
                .findFirst();
    }

    public String getSetCookieHeaderName() {
        return HttpHeaders.SET_COOKIE;
    }

    private String normalizeSameSite(String value) {
        if (!StringUtils.hasText(value)) {
            return "Lax";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> "Lax";
        };
    }
}
