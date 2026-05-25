package dev.taskflow.domain.port.repository;

import dev.taskflow.domain.model.Task;
import dev.taskflow.domain.model.TaskStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskRepository {

    Optional<Task> findById(UUID id);

    Page<Task> findByProjectId(UUID projectId, Pageable pageable);

    Page<Task> findByProjectIdAndStatus(UUID projectId, TaskStatus status, Pageable pageable);

    Page<Task> findByAssigneeId(UUID assigneeId, Pageable pageable);

    Task save(Task task);

    List<Task> findActiveByProjectId(UUID projectId);
}
