package dev.taskflow.infrastructure.persistence.repository;

import dev.taskflow.infrastructure.persistence.entity.TeamMembershipEntity;
import dev.taskflow.infrastructure.persistence.entity.TeamMembershipId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipJpaRepository extends JpaRepository<TeamMembershipEntity, TeamMembershipId> {

    List<TeamMembershipEntity> findByIdTeamId(UUID teamId);

    boolean existsByIdTeamIdAndIdUserId(UUID teamId, UUID userId);

    void deleteByIdTeamIdAndIdUserId(UUID teamId, UUID userId);
}
