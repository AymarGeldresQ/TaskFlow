package dev.taskflow.application.usecase.comment;

import dev.taskflow.application.dto.comment.CommentResponse;
import dev.taskflow.application.dto.common.PageResponse;
import dev.taskflow.domain.port.repository.CommentRepository;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ListCommentsUseCase {

    private final CommentRepository commentRepository;

    public ListCommentsUseCase(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public PageResponse<CommentResponse> execute(UUID taskId, Pageable pageable) {
        return PageResponse.from(commentRepository.findByTaskId(taskId, pageable).map(CommentResponse::from));
    }
}
