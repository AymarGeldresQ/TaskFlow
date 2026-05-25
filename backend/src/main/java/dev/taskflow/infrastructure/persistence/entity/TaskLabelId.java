package dev.taskflow.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TaskLabelId implements Serializable {

    @Column(name = "task_id", columnDefinition = "uuid", nullable = false)
    private UUID taskId;

    @Column(name = "label_id", columnDefinition = "uuid", nullable = false)
    private UUID labelId;

    protected TaskLabelId() {}

    public TaskLabelId(UUID taskId, UUID labelId) {
        this.taskId = taskId;
        this.labelId = labelId;
    }

    public UUID getTaskId() { return taskId; }
    public UUID getLabelId() { return labelId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof TaskLabelId that)) { return false; }
        return Objects.equals(taskId, that.taskId) && Objects.equals(labelId, that.labelId);
    }

    @Override
    public int hashCode() { return Objects.hash(taskId, labelId); }
}
