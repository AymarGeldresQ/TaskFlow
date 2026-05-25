package dev.taskflow.application.usecase.team;

import dev.taskflow.domain.exception.DomainException;
import dev.taskflow.domain.exception.EntityNotFoundException;
import dev.taskflow.domain.model.TeamMembership;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveTeamMemberUseCase {

    private final TeamMembershipRepository membershipRepository;

    public RemoveTeamMemberUseCase(TeamMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public void execute(UUID teamId, UUID targetUserId, UUID actorId) {
        TeamMembership actorMembership = membershipRepository.findByTeamIdAndUserId(teamId, actorId)
            .orElseThrow(() -> new DomainException("Actor is not a member of this team"));
        if (!actorMembership.getRole().canManageMembers()) {
            throw new dev.taskflow.domain.exception.UnauthorizedOperationException("manage team members");
        }
        TeamMembership target = membershipRepository.findByTeamIdAndUserId(teamId, targetUserId)
            .orElseThrow(() -> new EntityNotFoundException("membership", targetUserId));
        if (target.getRole() == dev.taskflow.domain.model.TeamRole.OWNER) {
            throw new DomainException("Cannot remove the team owner");
        }
        membershipRepository.delete(teamId, targetUserId);
    }
}
