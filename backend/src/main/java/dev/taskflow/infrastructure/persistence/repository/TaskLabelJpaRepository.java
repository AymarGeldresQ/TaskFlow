package dev.taskflow.infrastructure.persistence.repository;

import dev.taskflow.infrastructure.persistence.entity.TaskLabelEntity;
import dev.taskflow.infrastructure.persistence.entity.TaskLabelId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskLabelJpaRepository extends JpaRepository<TaskLabelEntity, TaskLabelId> {

    void deleteByIdTaskIdAndIdLabelId(java.util.UUID taskId, java.util.UUID labelId);
}
