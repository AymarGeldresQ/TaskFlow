package dev.taskflow.application.usecase.project;

import dev.taskflow.application.dto.project.ProjectResponse;
import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Project;
import dev.taskflow.domain.port.repository.ProjectRepository;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArchiveProjectUseCase {

    private final ProjectRepository projectRepository;
    private final TeamMembershipRepository membershipRepository;

    public ArchiveProjectUseCase(ProjectRepository projectRepository,
                                 TeamMembershipRepository membershipRepository) {
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public ProjectResponse execute(UUID projectId, UUID actorId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("project", projectId));
        membershipRepository.findByTeamIdAndUserId(project.getTeamId(), actorId)
            .filter(m -> m.getRole().canWriteTasks())
            .orElseThrow(() -> new dev.taskflow.domain.exception.UnauthorizedOperationException("archive project"));
        project.archive();
        return ProjectResponse.from(projectRepository.save(project));
    }
}
