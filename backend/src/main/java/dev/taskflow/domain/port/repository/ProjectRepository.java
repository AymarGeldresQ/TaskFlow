package dev.taskflow.domain.port.repository;

import dev.taskflow.domain.model.Project;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectRepository {

    Optional<Project> findById(UUID id);

    Page<Project> findByTeamId(UUID teamId, Pageable pageable);

    Project save(Project project);
}
