package dev.taskflow.integration;

import dev.taskflow.application.dto.label.CreateLabelRequest;
import dev.taskflow.application.dto.label.LabelResponse;
import dev.taskflow.application.dto.project.CreateProjectRequest;
import dev.taskflow.application.dto.project.ProjectResponse;
import dev.taskflow.application.dto.task.CreateTaskRequest;
import dev.taskflow.application.dto.task.TaskResponse;
import dev.taskflow.application.dto.team.CreateTeamRequest;
import dev.taskflow.application.dto.team.TeamResponse;
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

@DisplayName("Label API Integration Tests")
class LabelControllerIntegrationTest extends AbstractIntegrationTest {

    private String ownerToken;
    private UUID projectId;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        ownerToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Label Owner");
        String slug = "label-team-" + UUID.randomUUID();

        TeamResponse team = restTemplate.exchange(
            "/api/v1/teams", HttpMethod.POST,
            authRequest(new CreateTeamRequest("Label Team", slug), ownerToken),
            TeamResponse.class
        ).getBody();
        assertThat(team).isNotNull();

        ProjectResponse project = restTemplate.exchange(
            "/api/v1/teams/" + team.id() + "/projects", HttpMethod.POST,
            authRequest(new CreateProjectRequest("Label Project", null), ownerToken),
            ProjectResponse.class
        ).getBody();
        assertThat(project).isNotNull();
        projectId = project.id();

        TaskResponse task = restTemplate.exchange(
            "/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
            authRequest(new CreateTaskRequest("Task for labels", null, null, null), ownerToken),
            TaskResponse.class
        ).getBody();
        assertThat(task).isNotNull();
        taskId = task.id();
    }

    @Nested
    @DisplayName("POST /projects/{projectId}/labels")
    class CreateLabel {

        @Test
        void createsLabelInProject() {
            ResponseEntity<LabelResponse> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/labels", HttpMethod.POST,
                authRequest(new CreateLabelRequest("Bug", "#FF0000"), ownerToken),
                LabelResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().name()).isEqualTo("Bug");
            assertThat(response.getBody().color()).isEqualTo("#FF0000");
            assertThat(response.getBody().projectId()).isEqualTo(projectId);
            assertThat(response.getBody().id()).isNotNull();
        }

        @Test
        void returns400WhenColorFormatInvalid() {
            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/labels", HttpMethod.POST,
                authRequest(new CreateLabelRequest("Bad Color", "red"), ownerToken),
                Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns400WhenNameIsBlank() {
            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/labels", HttpMethod.POST,
                authRequest(new CreateLabelRequest("", "#00FF00"), ownerToken),
                Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns401WhenUnauthenticated() {
            ResponseEntity<Object> response = restTemplate.postForEntity(
                "/api/v1/projects/" + projectId + "/labels",
                new CreateLabelRequest("Feature", "#0000FF"),
                Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("GET /projects/{projectId}/labels")
    class ListLabels {

        @Test
        void returnsAllLabelsForProject() {
            restTemplate.exchange("/api/v1/projects/" + projectId + "/labels", HttpMethod.POST,
                authRequest(new CreateLabelRequest("Bug", "#FF0000"), ownerToken), LabelResponse.class);
            restTemplate.exchange("/api/v1/projects/" + projectId + "/labels", HttpMethod.POST,
                authRequest(new CreateLabelRequest("Feature", "#00FF00"), ownerToken), LabelResponse.class);

            ResponseEntity<List<LabelResponse>> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/labels", HttpMethod.GET,
                authRequest(ownerToken),
                new ParameterizedTypeReference<List<LabelResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().size()).isGreaterThanOrEqualTo(2);
        }

        @Test
        void returnsEmptyListWhenNoLabels() {
            String freshToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Fresh Owner");
            String freshSlug = "fresh-label-team-" + UUID.randomUUID();
            TeamResponse team = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.POST,
                authRequest(new CreateTeamRequest("Fresh Team", freshSlug), freshToken),
                TeamResponse.class
            ).getBody();
            assertThat(team).isNotNull();
            ProjectResponse project = restTemplate.exchange(
                "/api/v1/teams/" + team.id() + "/projects", HttpMethod.POST,
                authRequest(new CreateProjectRequest("Fresh Project", null), freshToken),
                ProjectResponse.class
            ).getBody();
            assertThat(project).isNotNull();

            ResponseEntity<List<LabelResponse>> response = restTemplate.exchange(
                "/api/v1/projects/" + project.id() + "/labels", HttpMethod.GET,
                authRequest(freshToken),
                new ParameterizedTypeReference<List<LabelResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("POST /tasks/{taskId}/labels/{labelId}")
    class AttachLabel {

        @Test
        void attachesLabelToTask() {
            LabelResponse label = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/labels", HttpMethod.POST,
                authRequest(new CreateLabelRequest("Urgent", "#FF6600"), ownerToken),
                LabelResponse.class
            ).getBody();
            assertThat(label).isNotNull();

            ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/labels/" + label.id(), HttpMethod.POST,
                authRequest(ownerToken), Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        void returns404WhenLabelDoesNotExist() {
            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/labels/" + UUID.randomUUID(), HttpMethod.POST,
                authRequest(ownerToken), Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void returns401WhenUnauthenticated() {
            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/labels/" + UUID.randomUUID(), HttpMethod.POST,
                null, Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("DELETE /tasks/{taskId}/labels/{labelId}")
    class DetachLabel {

        @Test
        void detachesLabelFromTask() {
            LabelResponse label = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/labels", HttpMethod.POST,
                authRequest(new CreateLabelRequest("To Detach", "#AABBCC"), ownerToken),
                LabelResponse.class
            ).getBody();
            assertThat(label).isNotNull();

            restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/labels/" + label.id(), HttpMethod.POST,
                authRequest(ownerToken), Void.class
            );

            ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/labels/" + label.id(), HttpMethod.DELETE,
                authRequest(ownerToken), Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }
}
