package dev.taskflow.domain.event;

import dev.taskflow.domain.model.TaskStatus;
import java.util.UUID;

public final class TaskStatusChangedEvent extends DomainEvent {

    private final UUID taskId;
    private final UUID projectId;
    private final TaskStatus from;
    private final TaskStatus to;
    private final UUID actorId;

    public TaskStatusChangedEvent(UUID taskId, UUID projectId, TaskStatus from, TaskStatus to, UUID actorId) {
        super();
        this.taskId = taskId;
        this.projectId = projectId;
        this.from = from;
        this.to = to;
        this.actorId = actorId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public TaskStatus getFrom() {
        return from;
    }

    public TaskStatus getTo() {
        return to;
    }

    public UUID getActorId() {
        return actorId;
    }
}
