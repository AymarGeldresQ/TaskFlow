package dev.taskflow.infrastructure.persistence.adapter;

import dev.taskflow.domain.model.Task;
import dev.taskflow.domain.model.TaskStatus;
import dev.taskflow.domain.port.repository.TaskRepository;
import dev.taskflow.infrastructure.persistence.mapper.TaskMapper;
import dev.taskflow.infrastructure.persistence.repository.TaskJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class TaskRepositoryAdapter implements TaskRepository {

    private final TaskJpaRepository jpaRepository;
    private final TaskMapper mapper;

    public TaskRepositoryAdapter(TaskJpaRepository jpaRepository, TaskMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(mapper::toDomain);
    }

    @Override
    public Page<Task> findByProjectId(UUID projectId, Pageable pageable) {
        return jpaRepository.findByProjectIdAndDeletedAtIsNull(projectId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Task> findByProjectIdAndStatus(UUID projectId, TaskStatus status, Pageable pageable) {
        return jpaRepository.findByProjectIdAndStatusAndDeletedAtIsNull(projectId, status, pageable)
            .map(mapper::toDomain);
    }

    @Override
    public Page<Task> findByAssigneeId(UUID assigneeId, Pageable pageable) {
        return jpaRepository.findByAssigneeIdAndDeletedAtIsNull(assigneeId, pageable).map(mapper::toDomain);
    }

    @Override
    public Task save(Task task) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(task)));
    }

    @Override
    public List<Task> findActiveByProjectId(UUID projectId) {
        return jpaRepository.findByProjectIdAndDeletedAtIsNull(projectId).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
