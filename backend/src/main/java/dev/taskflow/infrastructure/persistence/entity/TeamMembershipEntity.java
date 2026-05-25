package dev.taskflow.infrastructure.persistence.entity;

import dev.taskflow.domain.model.TeamRole;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "team_memberships")
public class TeamMembershipEntity {

    @EmbeddedId
    private TeamMembershipId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected TeamMembershipEntity() {}

    public TeamMembershipEntity(TeamMembershipId id, TeamRole role, Instant joinedAt) {
        this.id = id;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public TeamMembershipId getId() { return id; }
    public TeamRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }

    public void setRole(TeamRole role) { this.role = role; }
}
