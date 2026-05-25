package dev.taskflow.application.dto.team;

import dev.taskflow.domain.model.TeamRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMemberRequest(
    @NotNull UUID userId,
    @NotNull TeamRole role
) {}
