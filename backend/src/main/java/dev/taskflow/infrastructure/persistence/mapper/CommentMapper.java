package dev.taskflow.infrastructure.persistence.mapper;

import dev.taskflow.domain.model.Comment;
import dev.taskflow.infrastructure.persistence.entity.CommentEntity;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public Comment toDomain(CommentEntity entity) {
        return Comment.reconstitute(
            entity.getId(),
            entity.getTaskId(),
            entity.getAuthorId(),
            entity.getBody(),
            entity.getEditedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getDeletedAt()
        );
    }

    public CommentEntity toEntity(Comment domain) {
        return new CommentEntity(
            domain.getId(),
            domain.getTaskId(),
            domain.getAuthorId(),
            domain.getBody(),
            domain.getEditedAt(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getDeletedAt()
        );
    }
}
