package dev.taskflow.infrastructure.rest.controller;

import dev.taskflow.application.dto.comment.CommentResponse;
import dev.taskflow.application.dto.comment.CreateCommentRequest;
import dev.taskflow.application.dto.comment.UpdateCommentRequest;
import dev.taskflow.application.dto.common.PageResponse;
import dev.taskflow.application.usecase.comment.AddCommentUseCase;
import dev.taskflow.application.usecase.comment.DeleteCommentUseCase;
import dev.taskflow.application.usecase.comment.EditCommentUseCase;
import dev.taskflow.application.usecase.comment.ListCommentsUseCase;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Comments", description = "Task comments")
public class CommentController {

    private final AddCommentUseCase addCommentUseCase;
    private final ListCommentsUseCase listCommentsUseCase;
    private final EditCommentUseCase editCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;

    public CommentController(AddCommentUseCase addCommentUseCase,
                             ListCommentsUseCase listCommentsUseCase,
                             EditCommentUseCase editCommentUseCase,
                             DeleteCommentUseCase deleteCommentUseCase) {
        this.addCommentUseCase = addCommentUseCase;
        this.listCommentsUseCase = listCommentsUseCase;
        this.editCommentUseCase = editCommentUseCase;
        this.deleteCommentUseCase = deleteCommentUseCase;
    }

    @PostMapping("/tasks/{taskId}/comments")
    @Operation(summary = "Add a comment to a task")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(addCommentUseCase.execute(taskId, request, principal.getUserId()));
    }

    @GetMapping("/tasks/{taskId}/comments")
    @Operation(summary = "List comments for a task")
    public ResponseEntity<PageResponse<CommentResponse>> listComments(
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listCommentsUseCase.execute(taskId,
            PageRequest.of(page, size, Sort.by("createdAt").ascending())));
    }

    @PatchMapping("/comments/{commentId}")
    @Operation(summary = "Edit a comment")
    public ResponseEntity<CommentResponse> editComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.ok(editCommentUseCase.execute(commentId, request, principal.getUserId()));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal JwtUserDetails principal) {
        deleteCommentUseCase.execute(commentId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
