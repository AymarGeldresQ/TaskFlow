package dev.taskflow.application.usecase.team;

import dev.taskflow.application.dto.team.TeamMemberResponse;
import dev.taskflow.domain.port.repository.TeamMembershipRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetTeamMembersUseCase {

    private final TeamMembershipRepository membershipRepository;

    public GetTeamMembersUseCase(TeamMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public List<TeamMemberResponse> execute(UUID teamId) {
        return membershipRepository.findByTeamId(teamId).stream()
            .map(TeamMemberResponse::from)
            .toList();
    }
}
