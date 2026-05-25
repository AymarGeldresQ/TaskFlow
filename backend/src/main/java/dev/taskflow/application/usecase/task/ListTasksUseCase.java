package dev.taskflow.application.usecase.task;

import dev.taskflow.application.dto.common.PageResponse;
import dev.taskflow.application.dto.task.TaskResponse;
import dev.taskflow.domain.model.TaskStatus;
import dev.taskflow.domain.port.repository.TaskRepository;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ListTasksUseCase {

    private final TaskRepository taskRepository;

    public ListTasksUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public PageResponse<TaskResponse> execute(UUID projectId, TaskStatus status, Pageable pageable) {
        var page = status != null
            ? taskRepository.findByProjectIdAndStatus(projectId, status, pageable)
            : taskRepository.findByProjectId(projectId, pageable);
        return PageResponse.from(page.map(TaskResponse::from));
    }
}
