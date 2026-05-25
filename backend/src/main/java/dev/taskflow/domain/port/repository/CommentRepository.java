package dev.taskflow.domain.port.repository;

import dev.taskflow.domain.model.Comment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentRepository {

    Optional<Comment> findById(UUID id);

    Page<Comment> findByTaskId(UUID taskId, Pageable pageable);

    Comment save(Comment comment);
}
