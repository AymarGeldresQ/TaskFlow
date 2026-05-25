package dev.taskflow.infrastructure.rest.controller;

import dev.taskflow.application.dto.common.PageResponse;
import dev.taskflow.application.dto.task.AssignTaskRequest;
import dev.taskflow.application.dto.task.CreateTaskRequest;
import dev.taskflow.application.dto.task.TaskResponse;
import dev.taskflow.application.dto.task.TransitionTaskRequest;
import dev.taskflow.application.dto.task.UpdateTaskRequest;
import dev.taskflow.application.usecase.task.AssignTaskUseCase;
import dev.taskflow.application.usecase.task.CreateTaskUseCase;
import dev.taskflow.application.usecase.task.DeleteTaskUseCase;
import dev.taskflow.application.usecase.task.ListTasksUseCase;
import dev.taskflow.application.usecase.task.TransitionTaskStatusUseCase;
import dev.taskflow.application.usecase.task.UpdateTaskUseCase;
import dev.taskflow.domain.model.TaskStatus;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tasks", description = "Task management")
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final TransitionTaskStatusUseCase transitionTaskStatusUseCase;
    private final AssignTaskUseCase assignTaskUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;

    public TaskController(CreateTaskUseCase createTaskUseCase,
                          ListTasksUseCase listTasksUseCase,
                          UpdateTaskUseCase updateTaskUseCase,
                          TransitionTaskStatusUseCase transitionTaskStatusUseCase,
                          AssignTaskUseCase assignTaskUseCase,
                          DeleteTaskUseCase deleteTaskUseCase) {
        this.createTaskUseCase = createTaskUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.transitionTaskStatusUseCase = transitionTaskStatusUseCase;
        this.assignTaskUseCase = assignTaskUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Create a task in a project")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createTaskUseCase.execute(projectId, request, principal.getUserId()));
    }

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "List tasks in a project")
    public ResponseEntity<PageResponse<TaskResponse>> listTasks(
            @PathVariable UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listTasksUseCase.execute(projectId, status,
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PutMapping("/tasks/{taskId}")
    @Operation(summary = "Update task details")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.ok(updateTaskUseCase.execute(taskId, request, principal.getUserId()));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @Operation(summary = "Transition task status")
    public ResponseEntity<TaskResponse> transitionStatus(
            @PathVariable UUID taskId,
            @Valid @RequestBody TransitionTaskRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.ok(transitionTaskStatusUseCase.execute(taskId, request, principal.getUserId()));
    }

    @PatchMapping("/tasks/{taskId}/assignee")
    @Operation(summary = "Assign task to a user")
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable UUID taskId,
            @RequestBody AssignTaskRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.ok(assignTaskUseCase.execute(taskId, request, principal.getUserId()));
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Soft-delete a task")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal JwtUserDetails principal) {
        deleteTaskUseCase.execute(taskId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
