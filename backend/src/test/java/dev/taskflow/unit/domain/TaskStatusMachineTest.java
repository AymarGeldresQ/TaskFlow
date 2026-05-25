package dev.taskflow.unit.domain;

import dev.taskflow.domain.exception.InvalidTaskTransitionException;
import dev.taskflow.domain.model.Task;
import dev.taskflow.domain.model.TaskPriority;
import dev.taskflow.domain.model.TaskStatus;
import dev.taskflow.domain.event.DomainEvent;
import dev.taskflow.domain.event.TaskStatusChangedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Task State Machine")
class TaskStatusMachineTest {

    private UUID projectId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    private Task buildTask(TaskStatus initialStatus) {
        Task task = Task.create(projectId, "Test task", "desc", TaskPriority.MEDIUM, actorId);
        // Manually drain initial events
        task.pullDomainEvents();
        // Use reflection-free workaround: reconstitute at desired status
        return Task.reconstitute(
            UUID.randomUUID(), projectId, "Test task", "desc",
            initialStatus, TaskPriority.MEDIUM, null, null, actorId,
            java.time.Instant.now(), java.time.Instant.now(), null
        );
    }

    @Nested
    @DisplayName("Valid transitions")
    class ValidTransitions {

        @Test
        void backlogToTodo() {
            Task task = buildTask(TaskStatus.BACKLOG);
            task.transitionTo(TaskStatus.TODO, actorId);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        void backlogToCancelled() {
            Task task = buildTask(TaskStatus.BACKLOG);
            task.transitionTo(TaskStatus.CANCELLED, actorId);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        }

        @Test
        void todoToInProgress() {
            Task task = buildTask(TaskStatus.TODO);
            task.transitionTo(TaskStatus.IN_PROGRESS, actorId);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        void todoBackToBacklog() {
            Task task = buildTask(TaskStatus.TODO);
            task.transitionTo(TaskStatus.BACKLOG, actorId);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.BACKLOG);
        }

        @Test
        void inProgressToInReview() {
            Task task = buildTask(TaskStatus.IN_PROGRESS);
            task.transitionTo(TaskStatus.IN_REVIEW, actorId);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_REVIEW);
        }

        @Test
        void inProgressBackToTodo() {
            Task task = buildTask(TaskStatus.IN_PROGRESS);
            task.transitionTo(TaskStatus.TODO, actorId);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        void inReviewToDone() {
            Task task = buildTask(TaskStatus.IN_REVIEW);
            task.transitionTo(TaskStatus.DONE, actorId);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        void inReviewBackToInProgress() {
            Task task = buildTask(TaskStatus.IN_REVIEW);
            task.transitionTo(TaskStatus.IN_PROGRESS, actorId);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        @Test
        void backlogCannotJumpToInProgress() {
            Task task = buildTask(TaskStatus.BACKLOG);
            assertThatThrownBy(() -> task.transitionTo(TaskStatus.IN_PROGRESS, actorId))
                .isInstanceOf(InvalidTaskTransitionException.class)
                .hasMessageContaining("BACKLOG")
                .hasMessageContaining("IN_PROGRESS");
        }

        @Test
        void backlogCannotJumpToDone() {
            Task task = buildTask(TaskStatus.BACKLOG);
            assertThatThrownBy(() -> task.transitionTo(TaskStatus.DONE, actorId))
                .isInstanceOf(InvalidTaskTransitionException.class);
        }

        @Test
        void todoCannotJumpToInReview() {
            Task task = buildTask(TaskStatus.TODO);
            assertThatThrownBy(() -> task.transitionTo(TaskStatus.IN_REVIEW, actorId))
                .isInstanceOf(InvalidTaskTransitionException.class);
        }

        @Test
        void inProgressCannotJumpToDone() {
            Task task = buildTask(TaskStatus.IN_PROGRESS);
            assertThatThrownBy(() -> task.transitionTo(TaskStatus.DONE, actorId))
                .isInstanceOf(InvalidTaskTransitionException.class);
        }

        @ParameterizedTest
        @EnumSource(TaskStatus.class)
        void doneIsTerminal(TaskStatus target) {
            Task task = buildTask(TaskStatus.DONE);
            assertThatThrownBy(() -> task.transitionTo(target, actorId))
                .isInstanceOf(InvalidTaskTransitionException.class);
        }

        @ParameterizedTest
        @EnumSource(TaskStatus.class)
        void cancelledIsTerminal(TaskStatus target) {
            Task task = buildTask(TaskStatus.CANCELLED);
            assertThatThrownBy(() -> task.transitionTo(target, actorId))
                .isInstanceOf(InvalidTaskTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Domain events")
    class DomainEvents {

        @Test
        void transitionEmitsStatusChangedEvent() {
            Task task = buildTask(TaskStatus.BACKLOG);
            task.transitionTo(TaskStatus.TODO, actorId);

            List<DomainEvent> events = task.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(TaskStatusChangedEvent.class);

            TaskStatusChangedEvent event = (TaskStatusChangedEvent) events.get(0);
            assertThat(event.getFrom()).isEqualTo(TaskStatus.BACKLOG);
            assertThat(event.getTo()).isEqualTo(TaskStatus.TODO);
            assertThat(event.getActorId()).isEqualTo(actorId);
            assertThat(event.getTaskId()).isEqualTo(task.getId());
        }

        @Test
        void pullClearsPendingEvents() {
            Task task = buildTask(TaskStatus.BACKLOG);
            task.transitionTo(TaskStatus.TODO, actorId);

            task.pullDomainEvents(); // drain
            List<DomainEvent> secondPull = task.pullDomainEvents();

            assertThat(secondPull).isEmpty();
        }

        @Test
        void multipleTransitionsAccumulateEvents() {
            Task task = buildTask(TaskStatus.BACKLOG);
            task.transitionTo(TaskStatus.TODO, actorId);
            task.transitionTo(TaskStatus.IN_PROGRESS, actorId);

            List<DomainEvent> events = task.pullDomainEvents();
            assertThat(events).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Terminal state checks")
    class TerminalStates {

        @Test
        void doneIsTerminal() {
            assertThat(TaskStatus.DONE.isTerminal()).isTrue();
        }

        @Test
        void cancelledIsTerminal() {
            assertThat(TaskStatus.CANCELLED.isTerminal()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = TaskStatus.class, names = {"BACKLOG", "TODO", "IN_PROGRESS", "IN_REVIEW"})
        void activeStatusesAreNotTerminal(TaskStatus status) {
            assertThat(status.isTerminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("Task creation")
    class TaskCreation {

        @Test
        void newTaskStartsInBacklog() {
            Task task = Task.create(projectId, "My task", null, TaskPriority.HIGH, actorId);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.BACKLOG);
        }

        @Test
        void newTaskDefaultsPriorityToMediumWhenNull() {
            Task task = Task.create(projectId, "My task", null, null, actorId);
            assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        }

        @Test
        void blankTitleThrowsDomainException() {
            assertThatThrownBy(() -> Task.create(projectId, "  ", null, TaskPriority.MEDIUM, actorId))
                .isInstanceOf(dev.taskflow.domain.exception.DomainException.class);
        }

        @Test
        void nullProjectIdThrowsDomainException() {
            assertThatThrownBy(() -> Task.create(null, "Title", null, TaskPriority.MEDIUM, actorId))
                .isInstanceOf(dev.taskflow.domain.exception.DomainException.class);
        }
    }
}
