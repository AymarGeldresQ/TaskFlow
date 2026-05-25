package dev.taskflow.application.usecase.team;

import dev.taskflow.application.dto.team.AddMemberRequest;
import dev.taskflow.application.dto.team.TeamMemberResponse;
import dev.taskflow.domain.exception.DomainException;
import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.Team;
import dev.taskflow.domain.model.TeamMembership;
import dev.taskflow.domain.model.TeamRole;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import dev.taskflow.domain.port.repository.TeamRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddTeamMemberUseCase {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository membershipRepository;

    public AddTeamMemberUseCase(TeamRepository teamRepository,
                                TeamMembershipRepository membershipRepository) {
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public TeamMemberResponse execute(UUID teamId, AddMemberRequest request, UUID actorId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("team", teamId));
        TeamMembership actorMembership = membershipRepository.findByTeamIdAndUserId(teamId, actorId)
            .orElseThrow(() -> new DomainException("Actor is not a member of this team"));
        if (!actorMembership.getRole().canManageMembers()) {
            throw new dev.taskflow.domain.exception.UnauthorizedOperationException("manage team members");
        }
        if (!team.isActive()) {
            throw new DomainException("Cannot add member to a deleted team");
        }
        if (membershipRepository.existsByTeamIdAndUserId(teamId, request.userId())) {
            throw new DomainException("User is already a member of this team");
        }
        if (request.role() == TeamRole.OWNER) {
            throw new DomainException("Cannot assign OWNER role directly");
        }
        TeamMembership membership = TeamMembership.create(teamId, request.userId(), request.role());
        return TeamMemberResponse.from(membershipRepository.save(membership));
    }
}
