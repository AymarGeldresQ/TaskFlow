package dev.taskflow.infrastructure.rest.controller;

import dev.taskflow.application.dto.team.AddMemberRequest;
import dev.taskflow.application.dto.team.CreateTeamRequest;
import dev.taskflow.application.dto.team.TeamMemberResponse;
import dev.taskflow.application.dto.team.TeamResponse;
import dev.taskflow.application.usecase.team.AddTeamMemberUseCase;
import dev.taskflow.application.usecase.team.CreateTeamUseCase;
import dev.taskflow.application.usecase.team.GetTeamMembersUseCase;
import dev.taskflow.application.usecase.team.RemoveTeamMemberUseCase;
import dev.taskflow.infrastructure.security.JwtUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Team management")
public class TeamController {

    private final CreateTeamUseCase createTeamUseCase;
    private final AddTeamMemberUseCase addTeamMemberUseCase;
    private final RemoveTeamMemberUseCase removeTeamMemberUseCase;
    private final GetTeamMembersUseCase getTeamMembersUseCase;

    public TeamController(CreateTeamUseCase createTeamUseCase,
                          AddTeamMemberUseCase addTeamMemberUseCase,
                          RemoveTeamMemberUseCase removeTeamMemberUseCase,
                          GetTeamMembersUseCase getTeamMembersUseCase) {
        this.createTeamUseCase = createTeamUseCase;
        this.addTeamMemberUseCase = addTeamMemberUseCase;
        this.removeTeamMemberUseCase = removeTeamMemberUseCase;
        this.getTeamMembersUseCase = getTeamMembersUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new team")
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createTeamUseCase.execute(request, principal.getUserId()));
    }

    @GetMapping("/{teamId}/members")
    @Operation(summary = "List team members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(@PathVariable UUID teamId) {
        return ResponseEntity.ok(getTeamMembersUseCase.execute(teamId));
    }

    @PostMapping("/{teamId}/members")
    @Operation(summary = "Add a member to the team")
    public ResponseEntity<TeamMemberResponse> addMember(
            @PathVariable UUID teamId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(addTeamMemberUseCase.execute(teamId, request, principal.getUserId()));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @Operation(summary = "Remove a member from the team")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtUserDetails principal) {
        removeTeamMemberUseCase.execute(teamId, userId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
