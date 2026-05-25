package dev.taskflow.infrastructure.persistence.repository;

import dev.taskflow.infrastructure.persistence.entity.ProjectEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {

    Page<ProjectEntity> findByTeamIdAndDeletedAtIsNull(UUID teamId, Pageable pageable);

    Optional<ProjectEntity> findByIdAndDeletedAtIsNull(UUID id);
}
