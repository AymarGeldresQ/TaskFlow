package dev.taskflow.application.dto.task;

import dev.taskflow.domain.model.TaskPriority;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateTaskRequest(
    @Size(max = 500) String title,
    String description,
    TaskPriority priority,
    LocalDate dueDate
) {}
