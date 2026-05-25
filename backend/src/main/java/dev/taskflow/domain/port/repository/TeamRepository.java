package dev.taskflow.domain.port.repository;

import dev.taskflow.domain.model.Team;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository {

    Optional<Team> findById(UUID id);

    Optional<Team> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Team save(Team team);
}
