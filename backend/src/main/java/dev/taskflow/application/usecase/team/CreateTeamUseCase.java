package dev.taskflow.application.usecase.team;

import dev.taskflow.application.dto.team.CreateTeamRequest;
import dev.taskflow.application.dto.team.TeamResponse;
import dev.taskflow.domain.exception.DomainException;
import dev.taskflow.domain.model.Team;
import dev.taskflow.domain.model.TeamMembership;
import dev.taskflow.domain.model.TeamRole;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import dev.taskflow.domain.port.repository.TeamRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTeamUseCase {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository membershipRepository;

    public CreateTeamUseCase(TeamRepository teamRepository,
                             TeamMembershipRepository membershipRepository) {
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public TeamResponse execute(CreateTeamRequest request, UUID ownerId) {
        if (teamRepository.existsBySlug(request.slug())) {
            throw new DomainException("Team slug '" + request.slug() + "' is already taken");
        }
        Team team = Team.create(request.name(), request.slug(), ownerId);
        Team saved = teamRepository.save(team);
        membershipRepository.save(TeamMembership.create(saved.getId(), ownerId, TeamRole.OWNER));
        return TeamResponse.from(saved);
    }
}
