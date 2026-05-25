package dev.taskflow.infrastructure.persistence.mapper;

import dev.taskflow.domain.model.Team;
import dev.taskflow.infrastructure.persistence.entity.TeamEntity;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public Team toDomain(TeamEntity entity) {
        return Team.reconstitute(
            entity.getId(),
            entity.getName(),
            entity.getSlug(),
            entity.getOwnerId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getDeletedAt()
        );
    }

    public TeamEntity toEntity(Team domain) {
        return new TeamEntity(
            domain.getId(),
            domain.getName(),
            domain.getSlug(),
            domain.getOwnerId(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getDeletedAt()
        );
    }
}
