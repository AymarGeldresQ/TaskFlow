package dev.taskflow.application.dto.team;

import dev.taskflow.domain.model.TeamMembership;
import dev.taskflow.domain.model.TeamRole;
import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(UUID userId, TeamRole role, Instant joinedAt) {

    public static TeamMemberResponse from(TeamMembership m) {
        return new TeamMemberResponse(m.getUserId(), m.getRole(), m.getJoinedAt());
    }
}
