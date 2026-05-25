package dev.taskflow.application.dto.team;

import dev.taskflow.domain.model.Team;
import java.time.Instant;
import java.util.UUID;

public record TeamResponse(
    UUID id,
    String name,
    String slug,
    UUID ownerId,
    Instant createdAt
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getSlug(),
            team.getOwnerId(), team.getCreatedAt());
    }
}
