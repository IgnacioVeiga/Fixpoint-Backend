package com.fixpoint.auth.dto;

import java.time.OffsetDateTime;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        OffsetDateTime expiresAt,
        String username,
        String role
) {
}
