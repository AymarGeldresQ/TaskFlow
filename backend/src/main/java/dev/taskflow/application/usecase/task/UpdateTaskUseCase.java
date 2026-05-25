package dev.taskflow.application.usecase.task;

import dev.taskflow.application.dto.task.TaskResponse;
import dev.taskflow.application.dto.task.UpdateTaskRequest;
import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Project;
import dev.taskflow.domain.model.Task;
import dev.taskflow.domain.port.repository.ProjectRepository;
import dev.taskflow.domain.port.repository.TaskRepository;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateTaskUseCase {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TeamMembershipRepository membershipRepository;

    public UpdateTaskUseCase(TaskRepository taskRepository,
                             ProjectRepository projectRepository,
                             TeamMembershipRepository membershipRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public TaskResponse execute(UUID taskId, UpdateTaskRequest request, UUID actorId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("task", taskId));
        Project project = projectRepository.findById(task.getProjectId())
            .orElseThrow(() -> new EntityNotFoundException("project", task.getProjectId()));
        membershipRepository.findByTeamIdAndUserId(project.getTeamId(), actorId)
            .filter(m -> m.getRole().canWriteTasks())
            .orElseThrow(() -> new dev.taskflow.domain.exception.UnauthorizedOperationException("update task"));
        task.update(request.title(), request.description(), request.priority(), request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }
}
