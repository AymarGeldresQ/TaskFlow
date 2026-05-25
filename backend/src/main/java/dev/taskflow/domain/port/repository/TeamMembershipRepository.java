package dev.taskflow.domain.port.repository;

import dev.taskflow.domain.model.TeamMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMembershipRepository {

    Optional<TeamMembership> findByTeamIdAndUserId(UUID teamId, UUID userId);

    List<TeamMembership> findByTeamId(UUID teamId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    TeamMembership save(TeamMembership membership);

    void delete(UUID teamId, UUID userId);
}
