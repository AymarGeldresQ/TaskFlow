package dev.taskflow.application.usecase.task;

import dev.taskflow.application.dto.task.AssignTaskRequest;
import dev.taskflow.application.dto.task.TaskResponse;
import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Project;
import dev.taskflow.domain.model.Task;
import dev.taskflow.domain.port.repository.ProjectRepository;
import dev.taskflow.domain.port.repository.TaskRepository;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import dev.taskflow.domain.port.service.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignTaskUseCase {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TeamMembershipRepository membershipRepository;
    private final DomainEventPublisher eventPublisher;

    public AssignTaskUseCase(TaskRepository taskRepository,
                             ProjectRepository projectRepository,
                             TeamMembershipRepository membershipRepository,
                             DomainEventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TaskResponse execute(UUID taskId, AssignTaskRequest request, UUID actorId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("task", taskId));
        Project project = projectRepository.findById(task.getProjectId())
            .orElseThrow(() -> new EntityNotFoundException("project", task.getProjectId()));
        membershipRepository.findByTeamIdAndUserId(project.getTeamId(), actorId)
            .filter(m -> m.getRole().canWriteTasks())
            .orElseThrow(() -> new dev.taskflow.domain.exception.UnauthorizedOperationException("assign task"));
        task.assignTo(request.assigneeId(), actorId);
        Task saved = taskRepository.save(task);
        eventPublisher.publishAll(task.pullDomainEvents());
        return TaskResponse.from(saved);
    }
}
