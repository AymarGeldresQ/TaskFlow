package dev.taskflow.infrastructure.rest.controller;

import dev.taskflow.application.dto.label.CreateLabelRequest;
import dev.taskflow.application.dto.label.LabelResponse;
import dev.taskflow.application.usecase.label.AttachLabelUseCase;
import dev.taskflow.application.usecase.label.CreateLabelUseCase;
import dev.taskflow.application.usecase.label.DetachLabelUseCase;
import dev.taskflow.application.usecase.label.ListLabelsUseCase;
import dev.taskflow.infrastructure.security.JwtUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Labels", description = "Project labels and task label assignments")
public class LabelController {

    private final CreateLabelUseCase createLabelUseCase;
    private final ListLabelsUseCase listLabelsUseCase;
    private final AttachLabelUseCase attachLabelUseCase;
    private final DetachLabelUseCase detachLabelUseCase;

    public LabelController(CreateLabelUseCase createLabelUseCase,
                           ListLabelsUseCase listLabelsUseCase,
                           AttachLabelUseCase attachLabelUseCase,
                           DetachLabelUseCase detachLabelUseCase) {
        this.createLabelUseCase = createLabelUseCase;
        this.listLabelsUseCase = listLabelsUseCase;
        this.attachLabelUseCase = attachLabelUseCase;
        this.detachLabelUseCase = detachLabelUseCase;
    }

    @PostMapping("/projects/{projectId}/labels")
    @Operation(summary = "Create a label in a project")
    public ResponseEntity<LabelResponse> createLabel(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateLabelRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createLabelUseCase.execute(projectId, request, principal.getUserId()));
    }

    @GetMapping("/projects/{projectId}/labels")
    @Operation(summary = "List labels in a project")
    public ResponseEntity<List<LabelResponse>> listLabels(@PathVariable UUID projectId) {
        return ResponseEntity.ok(listLabelsUseCase.execute(projectId));
    }

    @PostMapping("/tasks/{taskId}/labels/{labelId}")
    @Operation(summary = "Attach a label to a task")
    public ResponseEntity<Void> attachLabel(
            @PathVariable UUID taskId,
            @PathVariable UUID labelId,
            @AuthenticationPrincipal JwtUserDetails principal) {
        attachLabelUseCase.execute(taskId, labelId, principal.getUserId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/tasks/{taskId}/labels/{labelId}")
    @Operation(summary = "Detach a label from a task")
    public ResponseEntity<Void> detachLabel(
            @PathVariable UUID taskId,
            @PathVariable UUID labelId,
            @AuthenticationPrincipal JwtUserDetails principal) {
        detachLabelUseCase.execute(taskId, labelId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
