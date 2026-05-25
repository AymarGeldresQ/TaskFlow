# Lesson 03 — Ports & Adapters (Repository Pattern)

## The Concept

A **port** is an interface that the domain defines to express a need. A **adapter** is the infrastructure implementation that fulfills that need.

Think of it like an electrical socket. The appliance (domain) defines what it needs: "I need 220V AC power." It doesn't care whether that comes from a power plant, a generator, or a battery inverter. The socket (port) is the contract. The power source (adapter) is the implementation.

In this project, the domain defines repository interfaces. The infrastructure provides JPA implementations.

## In This Project

### The Port (interface in the domain)

```java
// domain/port/repository/TaskRepository.java
public interface TaskRepository {
    Optional<Task> findById(UUID id);
    Page<Task> findByProjectId(UUID projectId, Pageable pageable);
    Page<Task> findByProjectIdAndStatus(UUID projectId, TaskStatus status, Pageable pageable);
    Task save(Task task);
    List<Task> findActiveByProjectId(UUID projectId);
}
```

Notice: this interface returns `Task` (domain object) — not `TaskEntity` (JPA object). The domain has no idea that JPA exists.

### The Adapter (JPA implementation in infrastructure)

```java
// infrastructure/persistence/adapter/TaskRepositoryAdapter.java
@Component
public class TaskRepositoryAdapter implements TaskRepository {

    private final TaskJpaRepository jpaRepository;  // Spring Data JPA
    private final TaskMapper mapper;

    @Override
    public Optional<Task> findById(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
            .map(mapper::toDomain);  // converts TaskEntity → Task
    }

    @Override
    public Task save(Task task) {
        return mapper.toDomain(
            jpaRepository.save(mapper.toEntity(task))  // Task → TaskEntity → save → TaskEntity → Task
        );
    }
}
```

### The Mapper (bridge between layers)

```java
// infrastructure/persistence/mapper/TaskMapper.java
@Component
public class TaskMapper {

    public Task toDomain(TaskEntity entity) {
        return Task.reconstitute(  // ← uses reconstitute(), NOT create()
            entity.getId(), entity.getProjectId(), entity.getTitle(),
            entity.getDescription(), entity.getStatus(), entity.getPriority(),
            entity.getAssigneeId(), entity.getDueDate(), entity.getCreatedBy(),
            entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt()
        );
    }

    public TaskEntity toEntity(Task domain) {
        return new TaskEntity(
            domain.getId(), domain.getProjectId(), domain.getTitle(),
            // ... all fields
        );
    }
}
```

### The JPA Entity (database shape)

```java
// infrastructure/persistence/entity/TaskEntity.java
@Entity
@Table(name = "tasks")
public class TaskEntity {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)  // stores "TODO", "DONE" etc in DB — not 0, 1, 2
    @Column(nullable = false, length = 20)
    private TaskStatus status;
    
    // ...
}
```

### The Spring Data JPA Repository

```java
// infrastructure/persistence/repository/TaskJpaRepository.java
public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {
    Optional<TaskEntity> findByIdAndDeletedAtIsNull(UUID id);
    Page<TaskEntity> findByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);
    Page<TaskEntity> findByProjectIdAndStatusAndDeletedAtIsNull(UUID projectId, TaskStatus status, Pageable pageable);
}
```

Spring Data JPA generates the SQL from method names at startup. No SQL written by hand.

## The Full Flow

When `CreateTaskUseCase.execute()` saves a task:

```
CreateTaskUseCase
  └── taskRepository.save(task)          ← domain interface
        └── TaskRepositoryAdapter.save()  ← adapter
              ├── mapper.toEntity(task)   ← Task → TaskEntity
              ├── jpaRepository.save()    ← JPA → SQL INSERT
              └── mapper.toDomain(entity) ← TaskEntity → Task (returned)
```

## Why This Many Classes?

It feels like a lot. `Task`, `TaskEntity`, `TaskMapper`, `TaskRepositoryAdapter`, `TaskJpaRepository`. Five classes for one concept. Here's the payoff:

**Scenario: You want to switch from Postgres to MongoDB.**
- Everything in `domain/` stays unchanged
- Everything in `application/` stays unchanged
- You swap `TaskEntity` for a MongoDB document class
- You swap `TaskRepositoryAdapter` for a MongoDB implementation
- The mapper might change format — that's it

**Scenario: You want to test `CreateTaskUseCase` without a database.**
```java
// In a unit test:
TaskRepository mockRepo = mock(TaskRepository.class);
when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

CreateTaskUseCase useCase = new CreateTaskUseCase(mockRepo, mockProjectRepo, mockMembershipRepo);
TaskResponse result = useCase.execute(projectId, request, userId);
// No Spring, no JPA, no database needed
```

The interface makes mocking trivial. If `CreateTaskUseCase` depended on `TaskRepositoryAdapter` directly, you couldn't do this.

## Key Files

| File | Role |
|------|------|
| `domain/port/repository/TaskRepository.java` | Port — what the domain needs |
| `infrastructure/persistence/adapter/TaskRepositoryAdapter.java` | Adapter — JPA implementation |
| `infrastructure/persistence/entity/TaskEntity.java` | JPA entity — database shape |
| `infrastructure/persistence/mapper/TaskMapper.java` | Mapper — converts between layers |
| `infrastructure/persistence/repository/TaskJpaRepository.java` | Spring Data JPA — SQL generation |
