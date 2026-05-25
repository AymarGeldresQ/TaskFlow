package dev.taskflow.application.usecase.project;

import dev.taskflow.application.dto.project.CreateProjectRequest;
import dev.taskflow.application.dto.project.ProjectResponse;
import dev.taskflow.domain.exception.DomainException;
import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Project;
import dev.taskflow.domain.model.Team;
import dev.taskflow.domain.port.repository.ProjectRepository;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import dev.taskflow.domain.port.repository.TeamRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProjectUseCase {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository membershipRepository;

    public CreateProjectUseCase(ProjectRepository projectRepository,
                                TeamRepository teamRepository,
                                TeamMembershipRepository membershipRepository) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public ProjectResponse execute(UUID teamId, CreateProjectRequest request, UUID actorId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("team", teamId));
        if (!team.isActive()) {
            throw new DomainException("Team is not active");
        }
        membershipRepository.findByTeamIdAndUserId(teamId, actorId)
            .filter(m -> m.getRole().canWriteTasks())
            .orElseThrow(() -> new dev.taskflow.domain.exception.UnauthorizedOperationException("create project"));
        Project project = Project.create(teamId, request.name(), request.description());
        return ProjectResponse.from(projectRepository.save(project));
    }
}
