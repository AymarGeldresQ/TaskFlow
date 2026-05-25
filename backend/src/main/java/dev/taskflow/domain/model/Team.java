package dev.taskflow.domain.model;

import dev.taskflow.domain.exception.DomainException;
import java.time.Instant;
import java.util.UUID;

public final class Team {

    private UUID id;
    private String name;
    private String slug;
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private Team() {}

    public static Team create(String name, String slug, UUID ownerId) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Team name cannot be blank");
        }
        if (slug == null || slug.isBlank() || !slug.matches("^[a-z0-9-]+$")) {
            throw new DomainException("Team slug must contain only lowercase letters, numbers, and hyphens");
        }
        Team team = new Team();
        team.id = UUID.randomUUID();
        team.name = name.trim();
        team.slug = slug.trim();
        team.ownerId = ownerId;
        team.createdAt = Instant.now();
        team.updatedAt = Instant.now();
        return team;
    }

    public static Team reconstitute(UUID id, String name, String slug, UUID ownerId,
                                    Instant createdAt, Instant updatedAt, Instant deletedAt) {
        Team team = new Team();
        team.id = id;
        team.name = name;
        team.slug = slug;
        team.ownerId = ownerId;
        team.createdAt = createdAt;
        team.updatedAt = updatedAt;
        team.deletedAt = deletedAt;
        return team;
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Team name cannot be blank");
        }
        this.name = name.trim();
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isActive() { return deletedAt == null; }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public UUID getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
