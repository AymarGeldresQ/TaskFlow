package dev.taskflow.application.dto.project;

import dev.taskflow.domain.model.Project;
import dev.taskflow.domain.model.ProjectStatus;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    UUID teamId,
    String name,
    String description,
    ProjectStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getTeamId(), project.getName(),
            project.getDescription(), project.getStatus(), project.getCreatedAt(), project.getUpdatedAt());
    }
}
