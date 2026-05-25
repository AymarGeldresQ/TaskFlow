package dev.taskflow.infrastructure.persistence.adapter;

import dev.taskflow.domain.model.Project;
import dev.taskflow.domain.port.repository.ProjectRepository;
import dev.taskflow.infrastructure.persistence.mapper.ProjectMapper;
import dev.taskflow.infrastructure.persistence.repository.ProjectJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectJpaRepository jpaRepository;
    private final ProjectMapper mapper;

    public ProjectRepositoryAdapter(ProjectJpaRepository jpaRepository, ProjectMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(mapper::toDomain);
    }

    @Override
    public Page<Project> findByTeamId(UUID teamId, Pageable pageable) {
        return jpaRepository.findByTeamIdAndDeletedAtIsNull(teamId, pageable).map(mapper::toDomain);
    }

    @Override
    public Project save(Project project) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(project)));
    }
}
