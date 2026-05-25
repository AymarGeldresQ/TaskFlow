package dev.taskflow.application.usecase.label;

import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Project;
import dev.taskflow.domain.port.repository.LabelRepository;
import dev.taskflow.domain.port.repository.ProjectRepository;
import dev.taskflow.domain.port.repository.TaskRepository;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetachLabelUseCase {

    private final LabelRepository labelRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TeamMembershipRepository membershipRepository;

    public DetachLabelUseCase(LabelRepository labelRepository,
                              TaskRepository taskRepository,
                              ProjectRepository projectRepository,
                              TeamMembershipRepository membershipRepository) {
        this.labelRepository = labelRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public void execute(UUID taskId, UUID labelId, UUID actorId) {
        var task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("task", taskId));
        Project project = projectRepository.findById(task.getProjectId())
            .orElseThrow(() -> new EntityNotFoundException("project", task.getProjectId()));
        membershipRepository.findByTeamIdAndUserId(project.getTeamId(), actorId)
            .filter(m -> m.getRole().canWriteTasks())
            .orElseThrow(() -> new dev.taskflow.domain.exception.UnauthorizedOperationException("detach label"));
        labelRepository.detachFromTask(taskId, labelId);
    }
}
