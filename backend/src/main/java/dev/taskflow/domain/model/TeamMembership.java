package dev.taskflow.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class TeamMembership {

    private UUID teamId;
    private UUID userId;
    private TeamRole role;
    private Instant joinedAt;

    private TeamMembership() {}

    public static TeamMembership create(UUID teamId, UUID userId, TeamRole role) {
        TeamMembership m = new TeamMembership();
        m.teamId = teamId;
        m.userId = userId;
        m.role = role;
        m.joinedAt = Instant.now();
        return m;
    }

    public static TeamMembership reconstitute(UUID teamId, UUID userId, TeamRole role, Instant joinedAt) {
        TeamMembership m = new TeamMembership();
        m.teamId = teamId;
        m.userId = userId;
        m.role = role;
        m.joinedAt = joinedAt;
        return m;
    }

    public UUID getTeamId() { return teamId; }
    public UUID getUserId() { return userId; }
    public TeamRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}
