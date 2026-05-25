package dev.taskflow.infrastructure.persistence.mapper;

import dev.taskflow.domain.model.TeamMembership;
import dev.taskflow.infrastructure.persistence.entity.TeamMembershipEntity;
import dev.taskflow.infrastructure.persistence.entity.TeamMembershipId;
import org.springframework.stereotype.Component;

@Component
public class TeamMembershipMapper {

    public TeamMembership toDomain(TeamMembershipEntity entity) {
        return TeamMembership.reconstitute(
            entity.getId().getTeamId(),
            entity.getId().getUserId(),
            entity.getRole(),
            entity.getJoinedAt()
        );
    }

    public TeamMembershipEntity toEntity(TeamMembership domain) {
        return new TeamMembershipEntity(
            new TeamMembershipId(domain.getTeamId(), domain.getUserId()),
            domain.getRole(),
            domain.getJoinedAt()
        );
    }
}
