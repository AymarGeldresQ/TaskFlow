package dev.taskflow.application.usecase.project;

import dev.taskflow.application.dto.common.PageResponse;
import dev.taskflow.application.dto.project.ProjectResponse;
import dev.taskflow.domain.port.repository.ProjectRepository;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ListProjectsUseCase {

    private final ProjectRepository projectRepository;

    public ListProjectsUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public PageResponse<ProjectResponse> execute(UUID teamId, Pageable pageable) {
        return PageResponse.from(
            projectRepository.findByTeamId(teamId, pageable).map(ProjectResponse::from)
        );
    }
}
