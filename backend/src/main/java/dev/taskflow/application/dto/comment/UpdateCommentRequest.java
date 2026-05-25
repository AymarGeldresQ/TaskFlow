package dev.taskflow.application.dto.comment;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(@NotBlank String body) {}
