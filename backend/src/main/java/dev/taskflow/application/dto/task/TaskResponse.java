package dev.taskflow.application.dto.task;

import dev.taskflow.domain.model.Task;
import dev.taskflow.domain.model.TaskPriority;
import dev.taskflow.domain.model.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    UUID projectId,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    UUID assigneeId,
    LocalDate dueDate,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(),
            task.getStatus(), task.getPriority(), task.getAssigneeId(), task.getDueDate(),
            task.getCreatedBy(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
