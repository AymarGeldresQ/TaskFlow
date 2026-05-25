package dev.taskflow.domain.model;

import dev.taskflow.domain.exception.DomainException;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

public final class Label {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private UUID id;
    private UUID projectId;
    private String name;
    private String color;
    private Instant createdAt;

    private Label() {}

    public static Label create(UUID projectId, String name, String color) {
        if (projectId == null) {
            throw new DomainException("Label must belong to a project");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Label name cannot be blank");
        }
        if (name.length() > 100) {
            throw new DomainException("Label name cannot exceed 100 characters");
        }
        if (color == null || !HEX_COLOR.matcher(color).matches()) {
            throw new DomainException("Label color must be a valid hex color (e.g. #FF5733)");
        }
        Label label = new Label();
        label.id = UUID.randomUUID();
        label.projectId = projectId;
        label.name = name.trim();
        label.color = color.toUpperCase();
        label.createdAt = Instant.now();
        return label;
    }

    public static Label reconstitute(UUID id, UUID projectId, String name, String color, Instant createdAt) {
        Label label = new Label();
        label.id = id;
        label.projectId = projectId;
        label.name = name;
        label.color = color;
        label.createdAt = createdAt;
        return label;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public Instant getCreatedAt() { return createdAt; }
}
