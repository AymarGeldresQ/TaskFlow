package dev.taskflow.integration;

import dev.taskflow.application.dto.common.PageResponse;
import dev.taskflow.application.dto.project.CreateProjectRequest;
import dev.taskflow.application.dto.project.ProjectResponse;
import dev.taskflow.application.dto.task.CreateTaskRequest;
import dev.taskflow.application.dto.task.TaskResponse;
import dev.taskflow.application.dto.task.TransitionTaskRequest;
import dev.taskflow.application.dto.task.UpdateTaskRequest;
import dev.taskflow.application.dto.team.CreateTeamRequest;
import dev.taskflow.application.dto.team.TeamResponse;
import dev.taskflow.domain.model.TaskPriority;
import dev.taskflow.domain.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task API Integration Tests")
class TaskControllerIntegrationTest extends AbstractIntegrationTest {

    private String ownerToken;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        ownerToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Task Owner");
        String slug = "task-team-" + UUID.randomUUID();
        TeamResponse team = restTemplate.exchange(
            "/api/v1/teams", HttpMethod.POST,
            authRequest(new CreateTeamRequest("Task Team", slug), ownerToken),
            TeamResponse.class
        ).getBody();
        assertThat(team).isNotNull();

        ProjectResponse project = restTemplate.exchange(
            "/api/v1/teams/" + team.id() + "/projects", HttpMethod.POST,
            authRequest(new CreateProjectRequest("Task Project", null), ownerToken),
            ProjectResponse.class
        ).getBody();
        assertThat(project).isNotNull();
        projectId = project.id();
    }

    @Nested
    @DisplayName("POST /projects/{projectId}/tasks")
    class CreateTask {

        @Test
        void createsTaskInProject() {
            CreateTaskRequest request = new CreateTaskRequest(
                "Implement login", "Add JWT authentication", TaskPriority.HIGH, null
            );

            ResponseEntity<TaskResponse> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(request, ownerToken), TaskResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().title()).isEqualTo("Implement login");
            assertThat(response.getBody().status()).isEqualTo(TaskStatus.BACKLOG);
            assertThat(response.getBody().priority()).isEqualTo(TaskPriority.HIGH);
            assertThat(response.getBody().projectId()).isEqualTo(projectId);
        }

        @Test
        void defaultsPriorityToMediumWhenNotSpecified() {
            CreateTaskRequest request = new CreateTaskRequest("Default priority task", null, null, null);

            ResponseEntity<TaskResponse> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(request, ownerToken), TaskResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().priority()).isEqualTo(TaskPriority.MEDIUM);
        }

        @Test
        void returns400WhenTitleIsBlank() {
            CreateTaskRequest request = new CreateTaskRequest("", null, null, null);

            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(request, ownerToken), Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /projects/{projectId}/tasks")
    class ListTasks {

        @Test
        void returnsPaginatedTasksForProject() {
            restTemplate.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(new CreateTaskRequest("Task A", null, null, null), ownerToken), TaskResponse.class);
            restTemplate.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(new CreateTaskRequest("Task B", null, TaskPriority.LOW, null), ownerToken), TaskResponse.class);

            ResponseEntity<PageResponse<TaskResponse>> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks?page=0&size=10", HttpMethod.GET,
                authRequest(ownerToken),
                new ParameterizedTypeReference<PageResponse<TaskResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().pagination().totalElements()).isGreaterThanOrEqualTo(2);
        }

        @Test
        void filtersTasksByStatus() {
            restTemplate.exchange("/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(new CreateTaskRequest("Backlog Task", null, null, null), ownerToken), TaskResponse.class);

            ResponseEntity<PageResponse<TaskResponse>> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks?status=BACKLOG&page=0&size=10", HttpMethod.GET,
                authRequest(ownerToken),
                new ParameterizedTypeReference<PageResponse<TaskResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().pagination().totalElements()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("PATCH /tasks/{taskId}/status")
    class TransitionStatus {

        @Test
        void transitionsFromBacklogToTodo() {
            TaskResponse task = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(new CreateTaskRequest("Transition Me", null, null, null), ownerToken),
                TaskResponse.class
            ).getBody();

            assertThat(task).isNotNull();

            ResponseEntity<TaskResponse> response = restTemplate.exchange(
                "/api/v1/tasks/" + task.id() + "/status", HttpMethod.PATCH,
                authRequest(new TransitionTaskRequest(TaskStatus.TODO), ownerToken),
                TaskResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        void returns422ForInvalidTransition() {
            TaskResponse task = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(new CreateTaskRequest("Invalid Transition", null, null, null), ownerToken),
                TaskResponse.class
            ).getBody();

            assertThat(task).isNotNull();

            // BACKLOG → DONE is invalid per the state machine
            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/tasks/" + task.id() + "/status", HttpMethod.PATCH,
                authRequest(new TransitionTaskRequest(TaskStatus.DONE), ownerToken),
                Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @Test
        void fullLifecycleBacklogToInReviewToDone() {
            TaskResponse task = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(new CreateTaskRequest("Full Lifecycle", null, null, null), ownerToken),
                TaskResponse.class
            ).getBody();

            assertThat(task).isNotNull();

            transition(task.id(), TaskStatus.TODO);
            transition(task.id(), TaskStatus.IN_PROGRESS);
            transition(task.id(), TaskStatus.IN_REVIEW);
            TaskResponse done = transition(task.id(), TaskStatus.DONE);

            assertThat(done.status()).isEqualTo(TaskStatus.DONE);
        }

        private TaskResponse transition(UUID taskId, TaskStatus status) {
            ResponseEntity<TaskResponse> resp = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/status", HttpMethod.PATCH,
                authRequest(new TransitionTaskRequest(status), ownerToken),
                TaskResponse.class
            );
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            return resp.getBody();
        }
    }

    @Nested
    @DisplayName("PUT /tasks/{taskId}")
    class UpdateTask {

        @Test
        void updatesTaskDetails() {
            TaskResponse task = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(new CreateTaskRequest("Original Title", null, null, null), ownerToken),
                TaskResponse.class
            ).getBody();

            assertThat(task).isNotNull();

            UpdateTaskRequest update = new UpdateTaskRequest(
                "Updated Title", "New description", TaskPriority.CRITICAL,
                LocalDate.now().plusDays(7)
            );

            ResponseEntity<TaskResponse> response = restTemplate.exchange(
                "/api/v1/tasks/" + task.id(), HttpMethod.PUT,
                authRequest(update, ownerToken), TaskResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().title()).isEqualTo("Updated Title");
            assertThat(response.getBody().priority()).isEqualTo(TaskPriority.CRITICAL);
        }
    }

    @Nested
    @DisplayName("DELETE /tasks/{taskId}")
    class DeleteTask {

        @Test
        void softDeletesTask() {
            TaskResponse task = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks", HttpMethod.POST,
                authRequest(new CreateTaskRequest("To Delete", null, null, null), ownerToken),
                TaskResponse.class
            ).getBody();

            assertThat(task).isNotNull();

            ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/tasks/" + task.id(), HttpMethod.DELETE,
                authRequest(ownerToken), Void.class
            );

            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // Task should no longer appear in list
            ResponseEntity<PageResponse<TaskResponse>> listResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/tasks?page=0&size=100", HttpMethod.GET,
                authRequest(ownerToken),
                new ParameterizedTypeReference<PageResponse<TaskResponse>>() {}
            );

            assertThat(listResponse.getBody()).isNotNull();
            boolean taskStillVisible = listResponse.getBody().data().stream()
                .anyMatch(t -> task.id().equals(t.id()));
            assertThat(taskStillVisible).isFalse();
        }
    }
}
