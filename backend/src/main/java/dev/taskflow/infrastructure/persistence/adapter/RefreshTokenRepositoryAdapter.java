package dev.taskflow.infrastructure.persistence.adapter;

import dev.taskflow.domain.model.RefreshToken;
import dev.taskflow.domain.port.repository.RefreshTokenRepository;
import dev.taskflow.infrastructure.persistence.entity.RefreshTokenEntity;
import dev.taskflow.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return toDomain(jpaRepository.save(toEntity(refreshToken)));
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        jpaRepository.revokeAllByUserId(userId, Instant.now());
    }

    @Override
    @Transactional
    public void deleteExpired() {
        jpaRepository.deleteExpiredBefore(Instant.now());
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.reconstitute(
            entity.getId(),
            entity.getUserId(),
            entity.getTokenHash(),
            entity.getExpiresAt(),
            entity.getRevokedAt(),
            entity.getCreatedAt()
        );
    }

    private RefreshTokenEntity toEntity(RefreshToken domain) {
        return new RefreshTokenEntity(
            domain.getId(),
            domain.getUserId(),
            domain.getTokenHash(),
            domain.getExpiresAt(),
            domain.getRevokedAt(),
            domain.getCreatedAt()
        );
    }
}
