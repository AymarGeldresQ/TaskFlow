package dev.taskflow.application.usecase.comment;

import dev.taskflow.application.dto.comment.CommentResponse;
import dev.taskflow.application.dto.comment.UpdateCommentRequest;
import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Comment;
import dev.taskflow.domain.port.repository.CommentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditCommentUseCase {

    private final CommentRepository commentRepository;

    public EditCommentUseCase(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Transactional
    public CommentResponse execute(UUID commentId, UpdateCommentRequest request, UUID actorId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("comment", commentId));
        comment.edit(request.body(), actorId);
        return CommentResponse.from(commentRepository.save(comment));
    }
}
