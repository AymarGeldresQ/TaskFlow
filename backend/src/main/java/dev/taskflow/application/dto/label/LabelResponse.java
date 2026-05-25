package dev.taskflow.application.dto.label;

import dev.taskflow.domain.model.Label;
import java.util.UUID;

public record LabelResponse(UUID id, UUID projectId, String name, String color) {

    public static LabelResponse from(Label label) {
        return new LabelResponse(label.getId(), label.getProjectId(), label.getName(), label.getColor());
    }
}
