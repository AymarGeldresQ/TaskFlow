package dev.taskflow.application.dto.task;

import java.util.UUID;

public record AssignTaskRequest(UUID assigneeId) {}
