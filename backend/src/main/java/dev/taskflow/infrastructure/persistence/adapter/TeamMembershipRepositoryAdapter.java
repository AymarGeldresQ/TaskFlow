package dev.taskflow.infrastructure.persistence.adapter;

import dev.taskflow.domain.model.TeamMembership;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import dev.taskflow.infrastructure.persistence.entity.TeamMembershipId;
import dev.taskflow.infrastructure.persistence.mapper.TeamMembershipMapper;
import dev.taskflow.infrastructure.persistence.repository.TeamMembershipJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TeamMembershipRepositoryAdapter implements TeamMembershipRepository {

    private final TeamMembershipJpaRepository jpaRepository;
    private final TeamMembershipMapper mapper;

    public TeamMembershipRepositoryAdapter(TeamMembershipJpaRepository jpaRepository,
                                           TeamMembershipMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<TeamMembership> findByTeamIdAndUserId(UUID teamId, UUID userId) {
        return jpaRepository.findById(new TeamMembershipId(teamId, userId)).map(mapper::toDomain);
    }

    @Override
    public List<TeamMembership> findByTeamId(UUID teamId) {
        return jpaRepository.findByIdTeamId(teamId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByTeamIdAndUserId(UUID teamId, UUID userId) {
        return jpaRepository.existsByIdTeamIdAndIdUserId(teamId, userId);
    }

    @Override
    public TeamMembership save(TeamMembership membership) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(membership)));
    }

    @Override
    @Transactional
    public void delete(UUID teamId, UUID userId) {
        jpaRepository.deleteByIdTeamIdAndIdUserId(teamId, userId);
    }
}
