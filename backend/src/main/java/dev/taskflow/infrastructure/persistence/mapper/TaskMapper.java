package dev.taskflow.infrastructure.persistence.mapper;

import dev.taskflow.domain.model.Task;
import dev.taskflow.infrastructure.persistence.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toDomain(TaskEntity entity) {
        return Task.reconstitute(
            entity.getId(),
            entity.getProjectId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getStatus(),
            entity.getPriority(),
            entity.getAssigneeId(),
            entity.getDueDate(),
            entity.getCreatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getDeletedAt()
        );
    }

    public TaskEntity toEntity(Task domain) {
        return new TaskEntity(
            domain.getId(),
            domain.getProjectId(),
            domain.getTitle(),
            domain.getDescription(),
            domain.getStatus(),
            domain.getPriority(),
            domain.getAssigneeId(),
            domain.getDueDate(),
            domain.getCreatedBy(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getDeletedAt()
        );
    }
}
