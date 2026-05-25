package dev.taskflow.infrastructure.event;

import dev.taskflow.domain.event.CommentAddedEvent;
import dev.taskflow.domain.event.TaskAssignedEvent;
import dev.taskflow.domain.event.TaskStatusChangedEvent;
import dev.taskflow.domain.model.AuditLog;
import dev.taskflow.domain.port.repository.AuditLogRepository;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AuditLogEventListener {

    private final AuditLogRepository auditLogRepository;

    public AuditLogEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    @EventListener
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        auditLogRepository.save(AuditLog.create(
            "task",
            event.getTaskId(),
            "STATUS_CHANGED",
            event.getActorId(),
            Map.of(
                "from", event.getFrom().name(),
                "to", event.getTo().name()
            )
        ));
    }

    @Async
    @EventListener
    public void onTaskAssigned(TaskAssignedEvent event) {
        auditLogRepository.save(AuditLog.create(
            "task",
            event.getTaskId(),
            "ASSIGNED",
            event.getActorId(),
            Map.of(
                "previousAssigneeId", event.getPreviousAssigneeId() != null
                    ? event.getPreviousAssigneeId().toString() : "none",
                "newAssigneeId", event.getNewAssigneeId() != null
                    ? event.getNewAssigneeId().toString() : "none"
            )
        ));
    }

    @Async
    @EventListener
    public void onCommentAdded(CommentAddedEvent event) {
        auditLogRepository.save(AuditLog.create(
            "task",
            event.getTaskId(),
            "COMMENT_ADDED",
            event.getAuthorId(),
            Map.of("commentId", event.getCommentId().toString())
        ));
    }
}
