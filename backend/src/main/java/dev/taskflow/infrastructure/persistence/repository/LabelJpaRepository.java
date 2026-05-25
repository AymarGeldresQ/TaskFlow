package dev.taskflow.infrastructure.persistence.repository;

import dev.taskflow.infrastructure.persistence.entity.LabelEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabelJpaRepository extends JpaRepository<LabelEntity, UUID> {

    List<LabelEntity> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    @Query(value = "SELECT l.* FROM labels l JOIN task_labels tl ON l.id = tl.label_id WHERE tl.task_id = :taskId", nativeQuery = true)
    List<LabelEntity> findByTaskId(@Param("taskId") UUID taskId);
}
