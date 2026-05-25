package dev.taskflow.domain.event;

import java.util.UUID;

public final class CommentAddedEvent extends DomainEvent {

    private final UUID commentId;
    private final UUID taskId;
    private final UUID authorId;

    public CommentAddedEvent(UUID commentId, UUID taskId, UUID authorId) {
        super();
        this.commentId = commentId;
        this.taskId = taskId;
        this.authorId = authorId;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getAuthorId() {
        return authorId;
    }
}
