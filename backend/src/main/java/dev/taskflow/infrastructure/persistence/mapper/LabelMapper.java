package dev.taskflow.infrastructure.persistence.mapper;

import dev.taskflow.domain.model.Label;
import dev.taskflow.infrastructure.persistence.entity.LabelEntity;
import org.springframework.stereotype.Component;

@Component
public class LabelMapper {

    public Label toDomain(LabelEntity entity) {
        return Label.reconstitute(
            entity.getId(),
            entity.getProjectId(),
            entity.getName(),
            entity.getColor(),
            entity.getCreatedAt()
        );
    }

    public LabelEntity toEntity(Label domain) {
        return new LabelEntity(
            domain.getId(),
            domain.getProjectId(),
            domain.getName(),
            domain.getColor(),
            domain.getCreatedAt()
        );
    }
}
