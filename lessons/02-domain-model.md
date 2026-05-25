# Lesson 02 — Domain Model & Aggregates

## The Concept

An **aggregate** is a cluster of objects treated as a single unit. The `Task` class is an aggregate. It owns its data, enforces its own invariants (rules), and controls how it changes. Nothing outside `Task` can mutate it directly — you go through its methods.

There are two ways to create a domain object, and they serve completely different purposes:

| Method | When | What it does |
|--------|------|--------------|
| `Task.create(...)` | A user creates a new task | Applies business rules, generates ID, sets defaults |
| `Task.reconstitute(...)` | Loading from database | Restores existing state — no validation, no events |

This distinction is critical. When you load from the DB, the data is already valid — it passed `create()` when it was first saved. Running validation again would be wasteful and wrong (e.g., you'd reject data that was valid under old rules).

## In This Project

```java
// domain/model/Task.java

public static Task create(UUID projectId, String title, String description,
                          TaskPriority priority, UUID createdBy) {
    // Business rules enforced HERE — at creation time
    if (title == null || title.isBlank()) {
        throw new DomainException("Task title cannot be blank");
    }
    if (title.length() > 500) {
        throw new DomainException("Task title cannot exceed 500 characters");
    }
    Task task = new Task();
    task.id = UUID.randomUUID();         // ID generated HERE — not by the DB
    task.status = TaskStatus.BACKLOG;    // default set HERE — not in the controller
    task.priority = priority != null ? priority : TaskPriority.MEDIUM; // default
    task.createdAt = Instant.now();
    return task;
}

public static Task reconstitute(UUID id, UUID projectId, String title, ...) {
    Task task = new Task();
    task.id = id;          // restored from DB — no validation
    task.status = status;  // whatever was in the DB
    // no rules, no events, no timestamps touched
    return task;
}
```

Notice the **private constructor**: `private Task() {}`. Nobody can do `new Task()`. You MUST go through `create()` or `reconstitute()`. This is intentional — it prevents creating an invalid object by forgetting to set required fields.

### Status transitions live in the domain

```java
// domain/model/TaskStatus.java
public enum TaskStatus {
    BACKLOG, TODO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED;

    public boolean canTransitionTo(TaskStatus target) {
        return switch (this) {
            case BACKLOG     -> target == TODO || target == CANCELLED;
            case TODO        -> target == IN_PROGRESS || target == BACKLOG || target == CANCELLED;
            case IN_PROGRESS -> target == IN_REVIEW || target == TODO || target == CANCELLED;
            case IN_REVIEW   -> target == DONE || target == IN_PROGRESS || target == CANCELLED;
            case DONE, CANCELLED -> false;
        };
    }
}
```

The state machine is defined **in the domain enum**, not in the controller, not in the use case. Why? Because any code that changes task status — whether it's a REST call, a background job, or a future GraphQL mutation — will use the same rules automatically. You can't bypass them.

### Domain events

```java
public void transitionTo(TaskStatus newStatus, UUID actorId) {
    status.validateTransitionTo(newStatus);   // throws if invalid
    TaskStatus previousStatus = this.status;
    this.status = newStatus;
    this.updatedAt = Instant.now();
    // Record that something happened
    domainEvents.add(new TaskStatusChangedEvent(id, projectId, previousStatus, newStatus, actorId));
}

public List<DomainEvent> pullDomainEvents() {
    List<DomainEvent> events = new ArrayList<>(domainEvents);
    domainEvents.clear();
    return Collections.unmodifiableList(events);
}
```

The task records that its status changed. After the use case saves the task to the DB, it calls `task.pullDomainEvents()` to drain the events and pass them to an event publisher. This is how you later add notifications, audit logs, or webhooks without touching this class.

## What Breaks Without It

Without a domain model, the logic ends up in controllers or service methods:

```java
// The common bad pattern
@PostMapping("/tasks/{id}/status")
public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody StatusRequest req) {
    Task task = taskRepo.findById(id).orElseThrow(...);
    
    // Status machine logic in the controller — breaks every time you add a new endpoint
    if (task.getStatus() == "DONE" && req.getStatus() == "IN_PROGRESS") {
        return ResponseEntity.badRequest().body("Can't move back from DONE");
    }
    // ... 20 more if-else cases scattered across multiple controllers
    
    task.setStatus(req.getStatus());
    taskRepo.save(task);
}
```

Problems:
- Every endpoint that touches status must duplicate the transition rules
- Unit testing the rules requires booting Spring
- Developers forget rules and create inconsistencies
- Adding a new status means hunting through all controllers

With the domain model, `transitionTo()` is tested in isolation with zero Spring:
```java
Task task = Task.create(projectId, "Fix bug", null, TaskPriority.HIGH, userId);
task.transitionTo(TaskStatus.TODO, userId);
task.transitionTo(TaskStatus.IN_PROGRESS, userId);
assertThrows(InvalidTaskTransitionException.class,
    () -> task.transitionTo(TaskStatus.BACKLOG, userId)); // BACKLOG not allowed from IN_PROGRESS
```

## Key Files

| File | What to study |
|------|---------------|
| `domain/model/Task.java` | `create()` vs `reconstitute()`, domain events |
| `domain/model/TaskStatus.java` | State machine in an enum |
| `domain/exception/DomainException.java` | Base exception for business rules |
| `domain/exception/InvalidTaskTransitionException.java` | Typed exception for bad transitions |
