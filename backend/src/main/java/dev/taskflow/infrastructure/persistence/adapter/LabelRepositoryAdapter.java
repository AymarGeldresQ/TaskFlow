package dev.taskflow.infrastructure.persistence.adapter;

import dev.taskflow.domain.model.Label;
import dev.taskflow.domain.port.repository.LabelRepository;
import dev.taskflow.infrastructure.persistence.entity.TaskLabelEntity;
import dev.taskflow.infrastructure.persistence.entity.TaskLabelId;
import dev.taskflow.infrastructure.persistence.mapper.LabelMapper;
import dev.taskflow.infrastructure.persistence.repository.LabelJpaRepository;
import dev.taskflow.infrastructure.persistence.repository.TaskLabelJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LabelRepositoryAdapter implements LabelRepository {

    private final LabelJpaRepository jpaRepository;
    private final TaskLabelJpaRepository taskLabelJpaRepository;
    private final LabelMapper mapper;

    public LabelRepositoryAdapter(LabelJpaRepository jpaRepository,
                                   TaskLabelJpaRepository taskLabelJpaRepository,
                                   LabelMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.taskLabelJpaRepository = taskLabelJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Label> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Label> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByProjectIdAndName(UUID projectId, String name) {
        return jpaRepository.existsByProjectIdAndName(projectId, name);
    }

    @Override
    public Label save(Label label) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(label)));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void attachToTask(UUID taskId, UUID labelId) {
        taskLabelJpaRepository.save(new TaskLabelEntity(new TaskLabelId(taskId, labelId)));
    }

    @Override
    @Transactional
    public void detachFromTask(UUID taskId, UUID labelId) {
        taskLabelJpaRepository.deleteByIdTaskIdAndIdLabelId(taskId, labelId);
    }

    @Override
    public List<Label> findByTaskId(UUID taskId) {
        return jpaRepository.findByTaskId(taskId).stream().map(mapper::toDomain).toList();
    }
}
