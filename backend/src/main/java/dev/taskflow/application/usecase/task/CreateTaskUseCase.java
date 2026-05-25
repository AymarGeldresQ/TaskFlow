package dev.taskflow.application.usecase.task;

import dev.taskflow.application.dto.task.CreateTaskRequest;
import dev.taskflow.application.dto.task.TaskResponse;
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
public class CreateTaskUseCase {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TeamMembershipRepository membershipRepository;

    public CreateTaskUseCase(TaskRepository taskRepository,
                             ProjectRepository projectRepository,
                             TeamMembershipRepository membershipRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public TaskResponse execute(UUID projectId, CreateTaskRequest request, UUID actorId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("project", projectId));
        membershipRepository.findByTeamIdAndUserId(project.getTeamId(), actorId)
            .filter(m -> m.getRole().canWriteTasks())
            .orElseThrow(() -> new dev.taskflow.domain.exception.UnauthorizedOperationException("create task"));
        Task task = Task.create(projectId, request.title(), request.description(),
            request.priority(), actorId);
        if (request.dueDate() != null) {
            task.update(null, null, null, request.dueDate());
        }
        return TaskResponse.from(taskRepository.save(task));
    }
}
