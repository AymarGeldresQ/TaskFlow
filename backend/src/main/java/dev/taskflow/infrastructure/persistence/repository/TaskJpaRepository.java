package dev.taskflow.infrastructure.persistence.repository;

import dev.taskflow.domain.model.TaskStatus;
import dev.taskflow.infrastructure.persistence.entity.TaskEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {

    Optional<TaskEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<TaskEntity> findByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);

    Page<TaskEntity> findByProjectIdAndStatusAndDeletedAtIsNull(UUID projectId, TaskStatus status, Pageable pageable);

    Page<TaskEntity> findByAssigneeIdAndDeletedAtIsNull(UUID assigneeId, Pageable pageable);

    List<TaskEntity> findByProjectIdAndDeletedAtIsNull(UUID projectId);

    long countByStatusNotInAndDeletedAtIsNull(Collection<TaskStatus> statuses);
}
