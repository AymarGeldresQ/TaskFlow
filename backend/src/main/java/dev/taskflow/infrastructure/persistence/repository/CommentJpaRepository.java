package dev.taskflow.infrastructure.persistence.repository;

import dev.taskflow.infrastructure.persistence.entity.CommentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentJpaRepository extends JpaRepository<CommentEntity, UUID> {

    Optional<CommentEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<CommentEntity> findByTaskIdAndDeletedAtIsNull(UUID taskId, Pageable pageable);
}
