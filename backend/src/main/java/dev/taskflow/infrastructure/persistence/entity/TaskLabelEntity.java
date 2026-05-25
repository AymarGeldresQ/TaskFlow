package dev.taskflow.infrastructure.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_labels")
public class TaskLabelEntity {

    @EmbeddedId
    private TaskLabelId id;

    protected TaskLabelEntity() {}

    public TaskLabelEntity(TaskLabelId id) {
        this.id = id;
    }

    public TaskLabelId getId() { return id; }
}
