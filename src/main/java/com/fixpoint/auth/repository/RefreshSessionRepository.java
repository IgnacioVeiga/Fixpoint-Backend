package com.fixpoint.auth.repository;

import com.fixpoint.auth.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    long deleteByExpiresAtBeforeOrRevokedAtBefore(LocalDateTime expiresBefore, LocalDateTime revokedBefore);
}
