# Lesson 01 — Hexagonal Architecture

## The Concept

Most beginners write code like this: the controller calls the repository, the repository has SQL, and the business logic lives... everywhere and nowhere.

Hexagonal Architecture (also called Clean Architecture or Ports & Adapters) says: **the business rules must not know anything about the outside world**. They must not know about Spring, JPA, HTTP, or Postgres. Why? Because those things change. Postgres gets swapped for MongoDB. REST gets replaced by GraphQL. Spring gets upgraded. Your business rules should survive all of that without a single change.

The shape of the architecture is three concentric rings:

```
┌──────────────────────────────────────────────┐
│              Infrastructure                   │  ← Spring, JPA, HTTP, DB
│   ┌──────────────────────────────────────┐   │
│   │            Application               │   │  ← Use cases, DTOs
│   │   ┌──────────────────────────────┐   │   │
│   │   │          Domain              │   │   │  ← Business rules ONLY
│   │   └──────────────────────────────┘   │   │
│   └──────────────────────────────────────┘   │
└──────────────────────────────────────────────┘
```

The rule: **dependencies point inward only**. Infrastructure knows about Application and Domain. Application knows about Domain. Domain knows about NOTHING outside itself.

## In This Project

Look at the package structure:

```
dev.taskflow/
├── domain/          ← Zero Spring/JPA imports. Pure Java.
│   ├── model/       Task, User, Team, Project, Comment...
│   ├── exception/   DomainException, EntityNotFoundException...
│   ├── event/       TaskStatusChangedEvent, TaskAssignedEvent...
│   └── port/
│       └── repository/  TaskRepository, UserRepository... (interfaces)
│
├── application/     ← Orchestrates domain. Spring @Service only.
│   ├── usecase/     CreateTaskUseCase, LoginUseCase...
│   └── dto/         Request/Response objects for the API
│
└── infrastructure/  ← Everything framework-specific
    ├── persistence/ JPA entities, repositories, mappers, adapters
    ├── rest/        Controllers, GlobalExceptionHandler
    ├── security/    JwtService, JwtAuthenticationFilter, SecurityConfig
    └── event/       SpringDomainEventPublisher
```

Open `Task.java`:

```java
// domain/model/Task.java
import dev.taskflow.domain.event.DomainEvent;
import dev.taskflow.domain.event.TaskAssignedEvent;
import dev.taskflow.domain.exception.DomainException;
import java.time.Instant;
// ...
```

Zero Spring imports. Zero JPA annotations. It's a POJO. This class runs identically in a Spring Boot app, a standalone CLI, or a unit test with no framework at all.

Now look at `TaskRepositoryAdapter.java`:

```java
// infrastructure/persistence/adapter/TaskRepositoryAdapter.java
@Component  // ← Spring annotation — only in infrastructure
public class TaskRepositoryAdapter implements TaskRepository {
    // implements the DOMAIN interface using JPA
}
```

The domain defines WHAT it needs (the `TaskRepository` interface). The infrastructure decides HOW to provide it (JPA, Postgres). The domain never knows the difference.

## What Breaks Without It

Say you skip this and put `@Entity` on `Task.java` directly (as many tutorials do):

```java
// The "simple" way that causes pain later
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;  // JPA loads this lazily... or eagerly... you never know
    
    public void transitionTo(TaskStatus newStatus) { ... }
}
```

Problems you will eventually hit:
1. `LazyInitializationException` — JPA loads relations lazily, so calling `task.getProject()` outside a transaction blows up
2. You can't test `transitionTo()` without spinning up a full Spring context
3. When you add a new DB column, Hibernate tries to auto-update the schema and corrupts production data
4. The entity becomes a god object with business logic AND persistence concerns mixed together

The hexagonal approach keeps `Task` clean. `TaskEntity` handles JPA. A mapper bridges them. It's more code — but each class has ONE job.

## Key Files

| File | Layer | What It Does |
|------|-------|--------------|
| `domain/model/Task.java` | Domain | Business object — pure Java |
| `domain/port/repository/TaskRepository.java` | Domain | Interface defining what persistence the domain needs |
| `infrastructure/persistence/entity/TaskEntity.java` | Infrastructure | JPA mapping — database shape |
| `infrastructure/persistence/mapper/TaskMapper.java` | Infrastructure | Converts between domain and JPA |
| `infrastructure/persistence/adapter/TaskRepositoryAdapter.java` | Infrastructure | Implements domain interface using JPA |
| `application/usecase/task/CreateTaskUseCase.java` | Application | Orchestrates domain + repository to create a task |
