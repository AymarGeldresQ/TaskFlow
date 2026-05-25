package dev.taskflow.domain.exception;

import dev.taskflow.domain.model.TaskStatus;

public class InvalidTaskTransitionException extends DomainException {

    private final TaskStatus from;
    private final TaskStatus to;

    public InvalidTaskTransitionException(TaskStatus from, TaskStatus to) {
        super(String.format("Cannot transition task from %s to %s", from, to));
        this.from = from;
        this.to = to;
    }

    public TaskStatus getFrom() {
        return from;
    }

    public TaskStatus getTo() {
        return to;
    }
}
