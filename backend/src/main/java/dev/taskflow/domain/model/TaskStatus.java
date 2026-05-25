package dev.taskflow.domain.model;

import dev.taskflow.domain.exception.InvalidTaskTransitionException;

public enum TaskStatus {

    BACKLOG,
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE,
    CANCELLED;

    public boolean canTransitionTo(TaskStatus target) {
        return switch (this) {
            case BACKLOG     -> target == TODO || target == CANCELLED;
            case TODO        -> target == IN_PROGRESS || target == BACKLOG || target == CANCELLED;
            case IN_PROGRESS -> target == IN_REVIEW || target == TODO || target == CANCELLED;
            case IN_REVIEW   -> target == DONE || target == IN_PROGRESS || target == CANCELLED;
            case DONE, CANCELLED -> false;
        };
    }

    public void validateTransitionTo(TaskStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidTaskTransitionException(this, target);
        }
    }

    public boolean isTerminal() {
        return this == DONE || this == CANCELLED;
    }
}
