package dev.taskflow.application.usecase.label;

import dev.taskflow.application.dto.label.CreateLabelRequest;
import dev.taskflow.application.dto.label.LabelResponse;
import dev.taskflow.domain.exception.DomainException;
import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Label;
import dev.taskflow.domain.model.Project;
import dev.taskflow.domain.port.repository.LabelRepository;
import dev.taskflow.domain.port.repository.ProjectRepository;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateLabelUseCase {

    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;
    private final TeamMembershipRepository membershipRepository;

    public CreateLabelUseCase(LabelRepository labelRepository,
                              ProjectRepository projectRepository,
                              TeamMembershipRepository membershipRepository) {
        this.labelRepository = labelRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public LabelResponse execute(UUID projectId, CreateLabelRequest request, UUID actorId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("project", projectId));
        membershipRepository.findByTeamIdAndUserId(project.getTeamId(), actorId)
            .filter(m -> m.getRole().canWriteTasks())
            .orElseThrow(() -> new dev.taskflow.domain.exception.UnauthorizedOperationException("create label"));
        if (labelRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new DomainException("Label '" + request.name() + "' already exists in this project");
        }
        Label label = Label.create(projectId, request.name(), request.color());
        return LabelResponse.from(labelRepository.save(label));
    }
}
