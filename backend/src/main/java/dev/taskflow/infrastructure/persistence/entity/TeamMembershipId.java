package dev.taskflow.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TeamMembershipId implements Serializable {

    @Column(name = "team_id", columnDefinition = "uuid", nullable = false)
    private UUID teamId;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    protected TeamMembershipId() {}

    public TeamMembershipId(UUID teamId, UUID userId) {
        this.teamId = teamId;
        this.userId = userId;
    }

    public UUID getTeamId() { return teamId; }
    public UUID getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof TeamMembershipId that)) { return false; }
        return Objects.equals(teamId, that.teamId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(teamId, userId); }
}
