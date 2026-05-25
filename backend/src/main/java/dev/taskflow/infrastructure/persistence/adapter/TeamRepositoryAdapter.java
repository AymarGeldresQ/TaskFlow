package dev.taskflow.infrastructure.persistence.adapter;

import dev.taskflow.domain.model.Team;
import dev.taskflow.domain.port.repository.TeamRepository;
import dev.taskflow.infrastructure.persistence.mapper.TeamMapper;
import dev.taskflow.infrastructure.persistence.repository.TeamJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TeamRepositoryAdapter implements TeamRepository {

    private final TeamJpaRepository jpaRepository;
    private final TeamMapper mapper;

    public TeamRepositoryAdapter(TeamJpaRepository jpaRepository, TeamMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Team> findBySlug(String slug) {
        return jpaRepository.findBySlugAndDeletedAtIsNull(slug).map(mapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlugAndDeletedAtIsNull(slug);
    }

    @Override
    public Team save(Team team) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(team)));
    }
}
