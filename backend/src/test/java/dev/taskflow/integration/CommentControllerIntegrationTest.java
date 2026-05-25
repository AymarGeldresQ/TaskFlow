package dev.taskflow.integration;

import dev.taskflow.application.dto.comment.CommentResponse;
import dev.taskflow.application.dto.comment.CreateCommentRequest;
import dev.taskflow.application.dto.comment.UpdateCommentRequest;
import dev.taskflow.application.dto.common.PageResponse;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Comment API Integration Tests")
class CommentControllerIntegrationTest extends AbstractIntegrationTest {

    private String ownerToken;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        ownerToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Comment Owner");
        String slug = "comment-team-" + UUID.randomUUID();

        TeamResponse team = restTemplate.exchange(
            "/api/v1/teams", HttpMethod.POST,
            authRequest(new CreateTeamRequest("Comment Team", slug), ownerToken),
            TeamResponse.class
        ).getBody();
        assertThat(team).isNotNull();

        ProjectResponse project = restTemplate.exchange(
            "/api/v1/teams/" + team.id() + "/projects", HttpMethod.POST,
            authRequest(new CreateProjectRequest("Comment Project", null), ownerToken),
            ProjectResponse.class
        ).getBody();
        assertThat(project).isNotNull();

        TaskResponse task = restTemplate.exchange(
            "/api/v1/projects/" + project.id() + "/tasks", HttpMethod.POST,
            authRequest(new CreateTaskRequest("Task for comments", null, null, null), ownerToken),
            TaskResponse.class
        ).getBody();
        assertThat(task).isNotNull();
        taskId = task.id();
    }

    @Nested
    @DisplayName("POST /tasks/{taskId}/comments")
    class AddComment {

        @Test
        void addsCommentToTask() {
            ResponseEntity<CommentResponse> response = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest("First comment body"), ownerToken),
                CommentResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().body()).isEqualTo("First comment body");
            assertThat(response.getBody().taskId()).isEqualTo(taskId);
            assertThat(response.getBody().id()).isNotNull();
        }

        @Test
        void returns400WhenBodyIsBlank() {
            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest(""), ownerToken),
                Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns401WhenUnauthenticated() {
            ResponseEntity<Object> response = restTemplate.postForEntity(
                "/api/v1/tasks/" + taskId + "/comments",
                new CreateCommentRequest("No auth"),
                Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void returns404WhenTaskDoesNotExist() {
            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/tasks/" + UUID.randomUUID() + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest("Ghost task comment"), ownerToken),
                Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /tasks/{taskId}/comments")
    class ListComments {

        @Test
        void returnsPagedComments() {
            restTemplate.exchange("/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest("Comment A"), ownerToken), CommentResponse.class);
            restTemplate.exchange("/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest("Comment B"), ownerToken), CommentResponse.class);

            ResponseEntity<PageResponse<CommentResponse>> response = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/comments?page=0&size=10", HttpMethod.GET,
                authRequest(ownerToken),
                new ParameterizedTypeReference<PageResponse<CommentResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().pagination().totalElements()).isGreaterThanOrEqualTo(2);
        }

        @Test
        void returnsEmptyPageWhenNoComments() {
            String noCommentToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Empty Owner");
            String noCommentSlug = "empty-comment-team-" + UUID.randomUUID();
            TeamResponse team = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.POST,
                authRequest(new CreateTeamRequest("Empty Team", noCommentSlug), noCommentToken),
                TeamResponse.class
            ).getBody();
            assertThat(team).isNotNull();
            ProjectResponse project = restTemplate.exchange(
                "/api/v1/teams/" + team.id() + "/projects", HttpMethod.POST,
                authRequest(new CreateProjectRequest("Empty Project", null), noCommentToken),
                ProjectResponse.class
            ).getBody();
            assertThat(project).isNotNull();
            TaskResponse task = restTemplate.exchange(
                "/api/v1/projects/" + project.id() + "/tasks", HttpMethod.POST,
                authRequest(new CreateTaskRequest("Empty Task", null, null, null), noCommentToken),
                TaskResponse.class
            ).getBody();
            assertThat(task).isNotNull();

            ResponseEntity<PageResponse<CommentResponse>> response = restTemplate.exchange(
                "/api/v1/tasks/" + task.id() + "/comments?page=0&size=10", HttpMethod.GET,
                authRequest(noCommentToken),
                new ParameterizedTypeReference<PageResponse<CommentResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().pagination().totalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("PATCH /comments/{commentId}")
    class EditComment {

        @Test
        void editsOwnComment() {
            CommentResponse comment = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest("Original body"), ownerToken),
                CommentResponse.class
            ).getBody();
            assertThat(comment).isNotNull();

            ResponseEntity<CommentResponse> response = restTemplate.exchange(
                "/api/v1/comments/" + comment.id(), HttpMethod.PATCH,
                authRequest(new UpdateCommentRequest("Updated body"), ownerToken),
                CommentResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().body()).isEqualTo("Updated body");
            assertThat(response.getBody().editedAt()).isNotNull();
        }

        @Test
        void returns403WhenEditingAnotherUsersComment() {
            CommentResponse comment = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest("Owner's comment"), ownerToken),
                CommentResponse.class
            ).getBody();
            assertThat(comment).isNotNull();

            String otherToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Other User");

            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/comments/" + comment.id(), HttpMethod.PATCH,
                authRequest(new UpdateCommentRequest("Hijacked body"), otherToken),
                Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void returns400WhenBodyIsBlank() {
            CommentResponse comment = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest("Valid body"), ownerToken),
                CommentResponse.class
            ).getBody();
            assertThat(comment).isNotNull();

            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/comments/" + comment.id(), HttpMethod.PATCH,
                authRequest(new UpdateCommentRequest(""), ownerToken),
                Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("DELETE /comments/{commentId}")
    class DeleteComment {

        @Test
        void deletesOwnComment() {
            CommentResponse comment = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest("To be deleted"), ownerToken),
                CommentResponse.class
            ).getBody();
            assertThat(comment).isNotNull();

            ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/comments/" + comment.id(), HttpMethod.DELETE,
                authRequest(ownerToken), Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        void returns403WhenDeletingAnotherUsersComment() {
            CommentResponse comment = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/comments", HttpMethod.POST,
                authRequest(new CreateCommentRequest("Not yours to delete"), ownerToken),
                CommentResponse.class
            ).getBody();
            assertThat(comment).isNotNull();

            String otherToken = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Other User");

            ResponseEntity<Object> response = restTemplate.exchange(
                "/api/v1/comments/" + comment.id(), HttpMethod.DELETE,
                authRequest(otherToken), Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
