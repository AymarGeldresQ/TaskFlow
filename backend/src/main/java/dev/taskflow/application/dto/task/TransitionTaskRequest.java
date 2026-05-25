package dev.taskflow.application.dto.task;

import dev.taskflow.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionTaskRequest(@NotNull TaskStatus status) {}
