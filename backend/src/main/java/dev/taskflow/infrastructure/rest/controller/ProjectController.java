package dev.taskflow.infrastructure.rest.controller;

import dev.taskflow.application.dto.common.PageResponse;
import dev.taskflow.application.dto.project.CreateProjectRequest;
import dev.taskflow.application.dto.project.ProjectResponse;
import dev.taskflow.application.usecase.project.ArchiveProjectUseCase;
import dev.taskflow.application.usecase.project.CreateProjectUseCase;
import dev.taskflow.application.usecase.project.ListProjectsUseCase;
import dev.taskflow.infrastructure.security.JwtUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Projects", description = "Project management")
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final ListProjectsUseCase listProjectsUseCase;
    private final ArchiveProjectUseCase archiveProjectUseCase;

    public ProjectController(CreateProjectUseCase createProjectUseCase,
                             ListProjectsUseCase listProjectsUseCase,
                             ArchiveProjectUseCase archiveProjectUseCase) {
        this.createProjectUseCase = createProjectUseCase;
        this.listProjectsUseCase = listProjectsUseCase;
        this.archiveProjectUseCase = archiveProjectUseCase;
    }

    @PostMapping("/teams/{teamId}/projects")
    @Operation(summary = "Create a project in a team")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createProjectUseCase.execute(teamId, request, principal.getUserId()));
    }

    @GetMapping("/teams/{teamId}/projects")
    @Operation(summary = "List projects in a team")
    public ResponseEntity<PageResponse<ProjectResponse>> listProjects(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listProjectsUseCase.execute(teamId,
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PatchMapping("/projects/{projectId}/archive")
    @Operation(summary = "Archive a project")
    public ResponseEntity<ProjectResponse> archiveProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.ok(archiveProjectUseCase.execute(projectId, principal.getUserId()));
    }
}
