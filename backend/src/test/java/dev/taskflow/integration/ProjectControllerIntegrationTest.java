package dev.taskflow.integration;

import dev.taskflow.application.dto.common.PageResponse;
import dev.taskflow.application.dto.project.CreateProjectRequest;
import dev.taskflow.application.dto.project.ProjectResponse;
import dev.taskflow.application.dto.team.CreateTeamRequest;
import dev.taskflow.application.dto.team.TeamResponse;
import dev.taskflow.domain.model.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Project API Integration Tests")
class ProjectControllerIntegrationTest extends AbstractIntegrationTest {

    private String ownerToken;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        ownerToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Project Owner");
        String slug = "proj-team-" + UUID.randomUUID();
        TeamResponse team = restTemplate.exchange(
            "/api/v1/teams", HttpMethod.POST,
            authRequest(new CreateTeamRequest("Project Team", slug), ownerToken),
            TeamResponse.class
        ).getBody();
        assertThat(team).isNotNull();
        teamId = team.id();
    }

    @Nested
    @DisplayName("POST /teams/{teamId}/projects")
    class CreateProject {

        @Test
        void createsProjectInTeam() {
            CreateProjectRequest request = new CreateProjectRequest("Backend API", "Core backend services");

            ResponseEntity<ProjectResponse> response = restTemplate.exchange(
                "/api/v1/teams/" + teamId + "/projects", HttpMethod.POST,
                authRequest(request, ownerToken), ProjectResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().name()).isEqualTo("Backend API");
            assertThat(response.getBody().status()).isEqualTo(ProjectStatus.ACTIVE);
            assertThat(response.getBody().teamId()).isEqualTo(teamId);
        }

        @Test
        void returns401WhenNotAuthenticated() {
            CreateProjectRequest request = new CreateProjectRequest("Anon Project", null);

            ResponseEntity<Object> response = restTemplate.postForEntity(
                "/api/v1/teams/" + teamId + "/projects", request, Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void returns400WhenNameIsBlank() {
            CreateProjectRequest request = new CreateProjectRequest("", null);

            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/teams/" + teamId + "/projects", HttpMethod.POST,
                authRequest(request, ownerToken), Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns404WhenTeamDoesNotExist() {
            CreateProjectRequest request = new CreateProjectRequest("Ghost Project", null);

            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/teams/" + UUID.randomUUID() + "/projects", HttpMethod.POST,
                authRequest(request, ownerToken), Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /teams/{teamId}/projects")
    class ListProjects {

        @Test
        void returnsPagedProjectsForTeam() {
            restTemplate.exchange("/api/v1/teams/" + teamId + "/projects", HttpMethod.POST,
                authRequest(new CreateProjectRequest("P1", null), ownerToken), ProjectResponse.class);
            restTemplate.exchange("/api/v1/teams/" + teamId + "/projects", HttpMethod.POST,
                authRequest(new CreateProjectRequest("P2", null), ownerToken), ProjectResponse.class);

            ResponseEntity<PageResponse<ProjectResponse>> response = restTemplate.exchange(
                "/api/v1/teams/" + teamId + "/projects?page=0&size=10", HttpMethod.GET,
                authRequest(ownerToken),
                new ParameterizedTypeReference<PageResponse<ProjectResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().pagination().totalElements()).isGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("PATCH /projects/{projectId}/archive")
    class ArchiveProject {

        @Test
        void archivesActiveProject() {
            ProjectResponse project = restTemplate.exchange(
                "/api/v1/teams/" + teamId + "/projects", HttpMethod.POST,
                authRequest(new CreateProjectRequest("To Archive", null), ownerToken),
                ProjectResponse.class
            ).getBody();

            assertThat(project).isNotNull();

            ResponseEntity<ProjectResponse> response = restTemplate.exchange(
                "/api/v1/projects/" + project.id() + "/archive", HttpMethod.PATCH,
                authRequest(ownerToken), ProjectResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(ProjectStatus.ARCHIVED);
        }

        @Test
        void returns422WhenAlreadyArchived() {
            ProjectResponse project = restTemplate.exchange(
                "/api/v1/teams/" + teamId + "/projects", HttpMethod.POST,
                authRequest(new CreateProjectRequest("Double Archive", null), ownerToken),
                ProjectResponse.class
            ).getBody();

            assertThat(project).isNotNull();

            restTemplate.exchange("/api/v1/projects/" + project.id() + "/archive",
                HttpMethod.PATCH, authRequest(ownerToken), ProjectResponse.class);

            ResponseEntity<Object> second = restTemplate.exchange(
                "/api/v1/projects/" + project.id() + "/archive",
                HttpMethod.PATCH, authRequest(ownerToken), Object.class
            );

            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
