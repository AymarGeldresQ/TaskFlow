package dev.taskflow.domain.port.repository;

import dev.taskflow.domain.model.Label;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepository {

    Optional<Label> findById(UUID id);

    List<Label> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    Label save(Label label);

    void deleteById(UUID id);

    void attachToTask(UUID taskId, UUID labelId);

    void detachFromTask(UUID taskId, UUID labelId);

    List<Label> findByTaskId(UUID taskId);
}
