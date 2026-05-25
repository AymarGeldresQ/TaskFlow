package dev.taskflow.domain.event;

import java.util.UUID;

public final class TaskAssignedEvent extends DomainEvent {

    private final UUID taskId;
    private final UUID projectId;
    private final UUID previousAssigneeId;
    private final UUID newAssigneeId;
    private final UUID actorId;

    public TaskAssignedEvent(UUID taskId, UUID projectId, UUID previousAssigneeId, UUID newAssigneeId, UUID actorId) {
        super();
        this.taskId = taskId;
        this.projectId = projectId;
        this.previousAssigneeId = previousAssigneeId;
        this.newAssigneeId = newAssigneeId;
        this.actorId = actorId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getPreviousAssigneeId() {
        return previousAssigneeId;
    }

    public UUID getNewAssigneeId() {
        return newAssigneeId;
    }

    public UUID getActorId() {
        return actorId;
    }
}
