package dev.taskflow.application.usecase.label;

import dev.taskflow.application.dto.label.LabelResponse;
import dev.taskflow.domain.port.repository.LabelRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ListLabelsUseCase {

    private final LabelRepository labelRepository;

    public ListLabelsUseCase(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    public List<LabelResponse> execute(UUID projectId) {
        return labelRepository.findByProjectId(projectId).stream().map(LabelResponse::from).toList();
    }
}
