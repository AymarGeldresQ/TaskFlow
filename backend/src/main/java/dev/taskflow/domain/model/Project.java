package dev.taskflow.domain.model;

import dev.taskflow.domain.exception.DomainException;
import java.time.Instant;
import java.util.UUID;

public final class Project {

    private UUID id;
    private UUID teamId;
    private String name;
    private String description;
    private ProjectStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private Project() {}

    public static Project create(UUID teamId, String name, String description) {
        if (teamId == null) {
            throw new DomainException("Project must belong to a team");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Project name cannot be blank");
        }
        Project project = new Project();
        project.id = UUID.randomUUID();
        project.teamId = teamId;
        project.name = name.trim();
        project.description = description;
        project.status = ProjectStatus.ACTIVE;
        project.createdAt = Instant.now();
        project.updatedAt = Instant.now();
        return project;
    }

    public static Project reconstitute(UUID id, UUID teamId, String name, String description,
                                       ProjectStatus status, Instant createdAt, Instant updatedAt,
                                       Instant deletedAt) {
        Project project = new Project();
        project.id = id;
        project.teamId = teamId;
        project.name = name;
        project.description = description;
        project.status = status;
        project.createdAt = createdAt;
        project.updatedAt = updatedAt;
        project.deletedAt = deletedAt;
        return project;
    }

    public void archive() {
        if (status == ProjectStatus.ARCHIVED) {
            throw new DomainException("Project is already archived");
        }
        this.status = ProjectStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = ProjectStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isActive() { return deletedAt == null && status == ProjectStatus.ACTIVE; }

    public UUID getId() { return id; }
    public UUID getTeamId() { return teamId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ProjectStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
