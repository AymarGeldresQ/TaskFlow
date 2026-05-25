package dev.taskflow.application.usecase.comment;

import dev.taskflow.application.dto.comment.CommentResponse;
import dev.taskflow.application.dto.comment.CreateCommentRequest;
import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Comment;
import dev.taskflow.domain.model.Project;
import dev.taskflow.domain.model.Task;
import dev.taskflow.domain.port.repository.CommentRepository;
import dev.taskflow.domain.port.repository.ProjectRepository;
import dev.taskflow.domain.port.repository.TaskRepository;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import dev.taskflow.domain.port.service.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddCommentUseCase {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TeamMembershipRepository membershipRepository;
    private final DomainEventPublisher eventPublisher;

    public AddCommentUseCase(CommentRepository commentRepository,
                             TaskRepository taskRepository,
                             ProjectRepository projectRepository,
                             TeamMembershipRepository membershipRepository,
                             DomainEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CommentResponse execute(UUID taskId, CreateCommentRequest request, UUID actorId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("task", taskId));
        Project project = projectRepository.findById(task.getProjectId())
            .orElseThrow(() -> new EntityNotFoundException("project", task.getProjectId()));
        membershipRepository.findByTeamIdAndUserId(project.getTeamId(), actorId)
            .orElseThrow(() -> new dev.taskflow.domain.exception.UnauthorizedOperationException("comment on task"));
        Comment comment = Comment.create(taskId, actorId, request.body());
        Comment saved = commentRepository.save(comment);
        eventPublisher.publishAll(comment.pullDomainEvents());
        return CommentResponse.from(saved);
    }
}
