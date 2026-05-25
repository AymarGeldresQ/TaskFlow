# Lesson 07 — Use Cases & `@Transactional`

## What Is a Use Case?

A use case is a single application operation — one thing the system can do. Each use case class has one public method: `execute()`. That method:
1. Loads what it needs from repositories
2. Enforces authorization
3. Delegates business logic to domain objects
4. Saves results
5. Returns a DTO

Use cases are the **application layer** — they coordinate without containing business rules themselves.

## In This Project

```java
// application/usecase/task/CreateTaskUseCase.java
@Service
public class CreateTaskUseCase {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TeamMembershipRepository membershipRepository;

    @Transactional
    public TaskResponse execute(UUID projectId, CreateTaskRequest request, UUID actorId) {

        // 1. Load what we need
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("project", projectId));

        // 2. Authorization check — who is allowed to do this?
        membershipRepository.findByTeamIdAndUserId(project.getTeamId(), actorId)
            .filter(m -> m.getRole().canWriteTasks())
            .orElseThrow(() -> new UnauthorizedOperationException("create task"));

        // 3. Delegate business logic to the domain model
        Task task = Task.create(projectId, request.title(), request.description(),
            request.priority(), actorId);

        // 4. Save
        return TaskResponse.from(taskRepository.save(task));
    }
}
```

Notice: the use case does NOT contain business rules like "title can't be blank" or "default priority is MEDIUM". Those live in `Task.create()`. The use case only orchestrates.

## `@Transactional` — Where and Why

`@Transactional` tells Spring: "wrap this method in a database transaction. If it succeeds, commit. If it throws, rollback."

It lives on the **use case** (application layer), NOT on:
- The domain model (domain has no Spring)
- The repository adapter (persistence knows nothing about business logic)
- The controller (HTTP layer shouldn't define transaction boundaries)

The use case IS the right boundary because:
- One HTTP request = one use case = one transaction
- If any step fails (authorization, domain validation, save), the whole operation rolls back atomically

### What happens without `@Transactional`?

```java
// Without @Transactional — DANGEROUS
public TaskResponse execute(...) {
    Project project = projectRepository.findById(projectId)...;  // DB connection A
    // ... check membership ...                                   // DB connection B
    Task task = Task.create(...);
    return TaskResponse.from(taskRepository.save(task));         // DB connection C
}
```

Three separate connections, three separate transactions. If the save fails after the authorization check, there's nothing to roll back (the auth check didn't write anything, so that's fine here — but in more complex scenarios with multiple writes, you'd have partial data).

With `@Transactional`:
```java
// With @Transactional — all reads and writes share ONE transaction
public TaskResponse execute(...) {
    // All three operations use the same DB connection, same transaction
    // If save() throws, the entire unit rolls back
}
```

### Read-Only Transactions

For use cases that only read data:

```java
@Transactional(readOnly = true)
public PageResponse<TaskResponse> execute(UUID projectId, ...) { ... }
```

`readOnly = true` is an optimization hint. Hibernate skips dirty-checking (tracking which entities changed), which improves performance for large result sets.

## How the Controller Gets the Current User

The controller needs to pass `actorId` to use cases. Spring Security provides it:

```java
// infrastructure/rest/controller/TaskController.java (simplified)
@PostMapping("/projects/{projectId}/tasks")
@ResponseStatus(HttpStatus.CREATED)
public TaskResponse createTask(
    @PathVariable UUID projectId,
    @Valid @RequestBody CreateTaskRequest request,
    @AuthenticationPrincipal JwtUserDetails currentUser  // ← Spring Security injection
) {
    return createTaskUseCase.execute(projectId, request, currentUser.getId());
}
```

`@AuthenticationPrincipal` tells Spring to inject the `UserDetails` that the JWT filter put in the `SecurityContextHolder`. That object is `JwtUserDetails`, which contains the user's UUID.

The controller doesn't do authorization ("is this user allowed?") — it just gets the current user and passes it to the use case. Authorization happens in the use case.

## One Use Case Per Class vs. Service Methods

Many tutorials put all task-related methods in a `TaskService`:

```java
// Common pattern — "service class"
@Service
public class TaskService {
    public TaskResponse createTask(...) { ... }
    public TaskResponse updateTask(...) { ... }
    public void deleteTask(...) { ... }
    public PageResponse<TaskResponse> listTasks(...) { ... }
    public TaskResponse transitionStatus(...) { ... }
    public TaskResponse assignTask(...) { ... }
}
```

This project uses one class per use case:
- `CreateTaskUseCase`
- `UpdateTaskUseCase`
- `DeleteTaskUseCase`
- `ListTasksUseCase`
- `TransitionTaskStatusUseCase`
- `AssignTaskUseCase`

Why?
- **Single Responsibility**: each class has ONE job
- **Dependency clarity**: `CreateTaskUseCase` depends on 3 repositories; `AssignTaskUseCase` depends on 2 different ones. A monolithic `TaskService` would depend on all of them even if you only call one method.
- **Testing**: test file for `CreateTaskUseCase` is small and focused
- **Growth**: as the app grows, `TaskService` becomes a 500-line file. Individual use cases stay small.

## Key Files

| File | What to study |
|------|---------------|
| `application/usecase/task/CreateTaskUseCase.java` | Standard use case structure |
| `application/usecase/task/TransitionTaskStatusUseCase.java` | Domain delegation + event publishing |
| `application/usecase/auth/LoginUseCase.java` | More complex orchestration with tokens |
| `application/dto/task/TaskResponse.java` | DTO — note the `from(Task)` factory method |
