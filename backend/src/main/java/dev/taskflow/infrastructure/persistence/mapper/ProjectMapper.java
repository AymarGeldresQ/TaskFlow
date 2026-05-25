package dev.taskflow.infrastructure.persistence.mapper;

import dev.taskflow.domain.model.Project;
import dev.taskflow.infrastructure.persistence.entity.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public Project toDomain(ProjectEntity entity) {
        return Project.reconstitute(
            entity.getId(),
            entity.getTeamId(),
            entity.getName(),
            entity.getDescription(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getDeletedAt()
        );
    }

    public ProjectEntity toEntity(Project domain) {
        return new ProjectEntity(
            domain.getId(),
            domain.getTeamId(),
            domain.getName(),
            domain.getDescription(),
            domain.getStatus(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getDeletedAt()
        );
    }
}
