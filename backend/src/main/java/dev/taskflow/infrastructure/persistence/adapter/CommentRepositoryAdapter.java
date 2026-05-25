package dev.taskflow.infrastructure.persistence.adapter;

import dev.taskflow.domain.model.Comment;
import dev.taskflow.domain.port.repository.CommentRepository;
import dev.taskflow.infrastructure.persistence.mapper.CommentMapper;
import dev.taskflow.infrastructure.persistence.repository.CommentJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class CommentRepositoryAdapter implements CommentRepository {

    private final CommentJpaRepository jpaRepository;
    private final CommentMapper mapper;

    public CommentRepositoryAdapter(CommentJpaRepository jpaRepository, CommentMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Comment> findById(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(mapper::toDomain);
    }

    @Override
    public Page<Comment> findByTaskId(UUID taskId, Pageable pageable) {
        return jpaRepository.findByTaskIdAndDeletedAtIsNull(taskId, pageable).map(mapper::toDomain);
    }

    @Override
    public Comment save(Comment comment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(comment)));
    }
}
