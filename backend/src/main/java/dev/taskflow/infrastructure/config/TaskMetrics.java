package dev.taskflow.infrastructure.config;

import dev.taskflow.domain.model.TaskStatus;
import dev.taskflow.infrastructure.persistence.repository.TaskJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TaskMetrics implements MeterBinder {

    private final TaskJpaRepository taskJpaRepository;

    public TaskMetrics(TaskJpaRepository taskJpaRepository) {
        this.taskJpaRepository = taskJpaRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("taskflow.tasks.active", taskJpaRepository,
                repo -> repo.countByStatusNotInAndDeletedAtIsNull(
                    List.of(TaskStatus.DONE, TaskStatus.CANCELLED)))
            .description("Number of active (non-terminal) tasks across all projects")
            .register(registry);
    }
}
