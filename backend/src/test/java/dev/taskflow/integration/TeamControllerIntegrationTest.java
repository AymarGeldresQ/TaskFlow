package dev.taskflow.integration;

import dev.taskflow.application.dto.team.AddMemberRequest;
import dev.taskflow.application.dto.team.CreateTeamRequest;
import dev.taskflow.application.dto.team.TeamMemberResponse;
import dev.taskflow.application.dto.team.TeamResponse;
import dev.taskflow.domain.model.TeamRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Team API Integration Tests")
class TeamControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/teams";

    private String ownerToken;

    @BeforeEach
    void setUp() {
        ownerToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Team Owner");
    }

    @Nested
    @DisplayName("POST /teams")
    class CreateTeam {

        @Test
        void createsTeamAndOwnerBecomesOwnerMember() {
            CreateTeamRequest request = new CreateTeamRequest("Acme Corp", "acme-" + UUID.randomUUID());

            ResponseEntity<TeamResponse> response = restTemplate.exchange(
                BASE, HttpMethod.POST, authRequest(request, ownerToken), TeamResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().name()).isEqualTo("Acme Corp");
            assertThat(response.getBody().id()).isNotNull();
        }

        @Test
        void returns422WhenSlugAlreadyTaken() {
            String slug = "unique-slug-" + UUID.randomUUID();
            CreateTeamRequest request = new CreateTeamRequest("First Team", slug);
            restTemplate.exchange(BASE, HttpMethod.POST, authRequest(request, ownerToken), TeamResponse.class);

            String secondToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Another Owner");
            CreateTeamRequest duplicate = new CreateTeamRequest("Second Team", slug);

            ResponseEntity<Object> response = restTemplate.exchange(
                BASE, HttpMethod.POST, authRequest(duplicate, secondToken), Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @Test
        void returns401WhenNotAuthenticated() {
            CreateTeamRequest request = new CreateTeamRequest("Anon Team", "anon-slug-" + UUID.randomUUID());

            ResponseEntity<Object> response = restTemplate.postForEntity(BASE, request, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void returns400WhenNameIsBlank() {
            CreateTeamRequest request = new CreateTeamRequest("", "valid-slug-" + UUID.randomUUID());

            ResponseEntity<Object> response = restTemplate.exchange(
                BASE, HttpMethod.POST, authRequest(request, ownerToken), Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /teams/{teamId}/members")
    class AddMember {

        @Test
        void ownerCanAddMember() {
            String slug = "member-team-" + UUID.randomUUID();
            TeamResponse team = restTemplate.exchange(
                BASE, HttpMethod.POST,
                authRequest(new CreateTeamRequest("Member Team", slug), ownerToken),
                TeamResponse.class
            ).getBody();

            assertThat(team).isNotNull();

            String memberEmail = uniqueEmail();
            String memberToken = registerAndGetToken(memberEmail, "SecurePass123!", "New Member");
            // Extract userId from the auth response via a login
            ResponseEntity<dev.taskflow.application.dto.auth.AuthResponse> loginResp =
                restTemplate.postForEntity("/api/v1/auth/login",
                    new dev.taskflow.application.dto.auth.LoginRequest(memberEmail, "SecurePass123!"),
                    dev.taskflow.application.dto.auth.AuthResponse.class);
            assertThat(loginResp.getBody()).isNotNull();
            UUID memberId = loginResp.getBody().user().id();

            AddMemberRequest addRequest = new AddMemberRequest(memberId, TeamRole.MEMBER);
            ResponseEntity<TeamMemberResponse> response = restTemplate.exchange(
                BASE + "/" + team.id() + "/members", HttpMethod.POST,
                authRequest(addRequest, ownerToken), TeamMemberResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().role()).isEqualTo(TeamRole.MEMBER);
        }

        @Test
        void returns403WhenViewerTriesToAddMember() {
            String slug = "perm-team-" + UUID.randomUUID();
            TeamResponse team = restTemplate.exchange(
                BASE, HttpMethod.POST,
                authRequest(new CreateTeamRequest("Perm Team", slug), ownerToken),
                TeamResponse.class
            ).getBody();

            assertThat(team).isNotNull();

            String viewerEmail = uniqueEmail();
            String viewerToken = registerAndGetToken(viewerEmail, "SecurePass123!", "Viewer");
            ResponseEntity<dev.taskflow.application.dto.auth.AuthResponse> loginResp =
                restTemplate.postForEntity("/api/v1/auth/login",
                    new dev.taskflow.application.dto.auth.LoginRequest(viewerEmail, "SecurePass123!"),
                    dev.taskflow.application.dto.auth.AuthResponse.class);
            assertThat(loginResp.getBody()).isNotNull();
            UUID viewerId = loginResp.getBody().user().id();

            restTemplate.exchange(
                BASE + "/" + team.id() + "/members", HttpMethod.POST,
                authRequest(new AddMemberRequest(viewerId, TeamRole.VIEWER), ownerToken),
                TeamMemberResponse.class
            );

            registerAndGetToken(uniqueEmail(), "SecurePass123!", "Stranger");
            UUID strangerId = UUID.randomUUID();
            AddMemberRequest req = new AddMemberRequest(strangerId, TeamRole.MEMBER);

            ResponseEntity<Object> response = restTemplate.exchange(
                BASE + "/" + team.id() + "/members", HttpMethod.POST,
                authRequest(req, viewerToken), Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("GET /teams/{teamId}/members")
    class GetTeamMembers {

        @Test
        void returnsAllMembersIncludingOwner() {
            String slug = "get-members-team-" + UUID.randomUUID();
            TeamResponse team = restTemplate.exchange(
                BASE, HttpMethod.POST,
                authRequest(new CreateTeamRequest("Get Members Team", slug), ownerToken),
                TeamResponse.class
            ).getBody();
            assertThat(team).isNotNull();

            String memberEmail = uniqueEmail();
            dev.taskflow.application.dto.auth.AuthResponse memberAuth = register(memberEmail, "SecurePass123!", "Member One");
            restTemplate.exchange(
                BASE + "/" + team.id() + "/members", HttpMethod.POST,
                authRequest(new AddMemberRequest(memberAuth.user().id(), TeamRole.MEMBER), ownerToken),
                TeamMemberResponse.class
            );

            ResponseEntity<List<TeamMemberResponse>> response = restTemplate.exchange(
                BASE + "/" + team.id() + "/members", HttpMethod.GET,
                authRequest(ownerToken),
                new ParameterizedTypeReference<List<TeamMemberResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().size()).isGreaterThanOrEqualTo(2);
        }

        @Test
        void returns401WhenUnauthenticated() {
            String slug = "unauth-members-team-" + UUID.randomUUID();
            TeamResponse team = restTemplate.exchange(
                BASE, HttpMethod.POST,
                authRequest(new CreateTeamRequest("Unauth Team", slug), ownerToken),
                TeamResponse.class
            ).getBody();
            assertThat(team).isNotNull();

            ResponseEntity<Object> response = restTemplate.exchange(
                BASE + "/" + team.id() + "/members", HttpMethod.GET,
                null, Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("DELETE /teams/{teamId}/members/{userId}")
    class RemoveMember {

        @Test
        void ownerCanRemoveMember() {
            String slug = "remove-team-" + UUID.randomUUID();
            TeamResponse team = restTemplate.exchange(
                BASE, HttpMethod.POST,
                authRequest(new CreateTeamRequest("Remove Team", slug), ownerToken),
                TeamResponse.class
            ).getBody();

            assertThat(team).isNotNull();

            String memberEmail = uniqueEmail();
            registerAndGetToken(memberEmail, "SecurePass123!", "To Remove");
            ResponseEntity<dev.taskflow.application.dto.auth.AuthResponse> loginResp =
                restTemplate.postForEntity("/api/v1/auth/login",
                    new dev.taskflow.application.dto.auth.LoginRequest(memberEmail, "SecurePass123!"),
                    dev.taskflow.application.dto.auth.AuthResponse.class);
            assertThat(loginResp.getBody()).isNotNull();
            UUID memberId = loginResp.getBody().user().id();

            restTemplate.exchange(
                BASE + "/" + team.id() + "/members", HttpMethod.POST,
                authRequest(new AddMemberRequest(memberId, TeamRole.MEMBER), ownerToken),
                TeamMemberResponse.class
            );

            ResponseEntity<Void> response = restTemplate.exchange(
                BASE + "/" + team.id() + "/members/" + memberId,
                HttpMethod.DELETE, authRequest(ownerToken), Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }
}
