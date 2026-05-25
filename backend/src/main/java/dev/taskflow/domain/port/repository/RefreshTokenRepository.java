package dev.taskflow.domain.port.repository;

import dev.taskflow.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken refreshToken);

    void revokeAllForUser(UUID userId);

    void deleteExpired();
}
