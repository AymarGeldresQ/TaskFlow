package dev.taskflow.application.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") @Size(max = 255) String slug
) {}
