package dev.taskflow.domain.model;

import dev.taskflow.domain.event.DomainEvent;
import dev.taskflow.domain.event.TaskAssignedEvent;
import dev.taskflow.domain.event.TaskStatusChangedEvent;
import dev.taskflow.domain.exception.DomainException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Task {

    private UUID id;
    private UUID projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private UUID assigneeId;
    private LocalDate dueDate;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Task() {}

    public static Task create(UUID projectId, String title, String description,
                              TaskPriority priority, UUID createdBy) {
        if (projectId == null) {
            throw new DomainException("Task must belong to a project");
        }
        if (title == null || title.isBlank()) {
            throw new DomainException("Task title cannot be blank");
        }
        if (title.length() > 500) {
            throw new DomainException("Task title cannot exceed 500 characters");
        }
        if (createdBy == null) {
            throw new DomainException("Task must have a creator");
        }
        Task task = new Task();
        task.id = UUID.randomUUID();
        task.projectId = projectId;
        task.title = title.trim();
        task.description = description;
        task.status = TaskStatus.BACKLOG;
        task.priority = priority != null ? priority : TaskPriority.MEDIUM;
        task.createdBy = createdBy;
        task.createdAt = Instant.now();
        task.updatedAt = Instant.now();
        return task;
    }

    public static Task reconstitute(UUID id, UUID projectId, String title, String description,
                                     TaskStatus status, TaskPriority priority, UUID assigneeId,
                                     LocalDate dueDate, UUID createdBy, Instant createdAt,
                                     Instant updatedAt, Instant deletedAt) {
        Task task = new Task();
        task.id = id;
        task.projectId = projectId;
        task.title = title;
        task.description = description;
        task.status = status;
        task.priority = priority;
        task.assigneeId = assigneeId;
        task.dueDate = dueDate;
        task.createdBy = createdBy;
        task.createdAt = createdAt;
        task.updatedAt = updatedAt;
        task.deletedAt = deletedAt;
        return task;
    }

    public void transitionTo(TaskStatus newStatus, UUID actorId) {
        status.validateTransitionTo(newStatus);
        TaskStatus previousStatus = this.status;
        this.status = newStatus;
        this.updatedAt = Instant.now();
        domainEvents.add(new TaskStatusChangedEvent(id, projectId, previousStatus, newStatus, actorId));
    }

    public void assignTo(UUID newAssigneeId, UUID actorId) {
        UUID previousAssigneeId = this.assigneeId;
        this.assigneeId = newAssigneeId;
        this.updatedAt = Instant.now();
        domainEvents.add(new TaskAssignedEvent(id, projectId, previousAssigneeId, newAssigneeId, actorId));
    }

    public void update(String title, String description, TaskPriority priority, LocalDate dueDate) {
        if (status.isTerminal()) {
            throw new DomainException("Cannot update a task in terminal status: " + status);
        }
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
        this.description = description;
        if (priority != null) {
            this.priority = priority;
        }
        this.dueDate = dueDate;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** Drains and returns pending domain events — call after persisting. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public boolean isActive() { return deletedAt == null; }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public TaskPriority getPriority() { return priority; }
    public UUID getAssigneeId() { return assigneeId; }
    public LocalDate getDueDate() { return dueDate; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
