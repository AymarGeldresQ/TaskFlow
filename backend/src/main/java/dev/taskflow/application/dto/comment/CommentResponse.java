package dev.taskflow.application.dto.comment;

import dev.taskflow.domain.model.Comment;
import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID taskId,
    UUID authorId,
    String body,
    Instant editedAt,
    Instant createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getTaskId(), comment.getAuthorId(),
            comment.getBody(), comment.getEditedAt(), comment.getCreatedAt());
    }
}
