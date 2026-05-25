package dev.taskflow.domain.model;

import dev.taskflow.domain.event.CommentAddedEvent;
import dev.taskflow.domain.exception.DomainException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Comment {

    private UUID id;
    private UUID taskId;
    private UUID authorId;
    private String body;
    private Instant editedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private final List<dev.taskflow.domain.event.DomainEvent> domainEvents = new ArrayList<>();

    private Comment() {}

    public static Comment create(UUID taskId, UUID authorId, String body) {
        if (taskId == null) {
            throw new DomainException("Comment must belong to a task");
        }
        if (authorId == null) {
            throw new DomainException("Comment must have an author");
        }
        if (body == null || body.isBlank()) {
            throw new DomainException("Comment body cannot be blank");
        }
        Comment comment = new Comment();
        comment.id = UUID.randomUUID();
        comment.taskId = taskId;
        comment.authorId = authorId;
        comment.body = body.trim();
        comment.createdAt = Instant.now();
        comment.updatedAt = Instant.now();
        comment.domainEvents.add(new CommentAddedEvent(comment.id, taskId, authorId));
        return comment;
    }

    public static Comment reconstitute(UUID id, UUID taskId, UUID authorId, String body,
                                       Instant editedAt, Instant createdAt, Instant updatedAt,
                                       Instant deletedAt) {
        Comment comment = new Comment();
        comment.id = id;
        comment.taskId = taskId;
        comment.authorId = authorId;
        comment.body = body;
        comment.editedAt = editedAt;
        comment.createdAt = createdAt;
        comment.updatedAt = updatedAt;
        comment.deletedAt = deletedAt;
        return comment;
    }

    public void edit(String newBody, UUID editorId) {
        if (deletedAt != null) {
            throw new DomainException("Cannot edit a deleted comment");
        }
        if (!authorId.equals(editorId)) {
            throw new dev.taskflow.domain.exception.UnauthorizedOperationException("edit comment");
        }
        if (newBody == null || newBody.isBlank()) {
            throw new DomainException("Comment body cannot be blank");
        }
        this.body = newBody.trim();
        this.editedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void softDelete(UUID requesterId) {
        if (!authorId.equals(requesterId)) {
            throw new dev.taskflow.domain.exception.UnauthorizedOperationException("delete comment");
        }
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public List<dev.taskflow.domain.event.DomainEvent> pullDomainEvents() {
        List<dev.taskflow.domain.event.DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public boolean isActive() { return deletedAt == null; }

    public UUID getId() { return id; }
    public UUID getTaskId() { return taskId; }
    public UUID getAuthorId() { return authorId; }
    public String getBody() { return body; }
    public Instant getEditedAt() { return editedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
