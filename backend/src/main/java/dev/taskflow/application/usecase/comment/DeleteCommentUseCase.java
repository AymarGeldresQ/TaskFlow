package dev.taskflow.application.usecase.comment;

import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Comment;
import dev.taskflow.domain.port.repository.CommentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteCommentUseCase {

    private final CommentRepository commentRepository;

    public DeleteCommentUseCase(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Transactional
    public void execute(UUID commentId, UUID actorId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("comment", commentId));
        comment.softDelete(actorId);
        commentRepository.save(comment);
    }
}
