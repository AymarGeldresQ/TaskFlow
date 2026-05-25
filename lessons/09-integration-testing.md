# Lesson 09 — Integration Testing

## Why Not Mock the Database?

Unit tests use mocks. Integration tests use a REAL database. This is intentional and important.

When you mock the repository:
```java
when(taskRepo.save(any())).thenReturn(task);
```
You're testing that your code calls the right methods in the right order. You're NOT testing:
- That the SQL actually executes
- That your `@Column(nullable = false)` constraints are enforced
- That Flyway migrations are correct
- That Spring Data JPA method names (`findByProjectIdAndDeletedAtIsNull`) are valid
- That transactions commit/rollback correctly

This project burned by this exact problem in a previous project: mocked tests passed, the production migration broke on a column rename. Integration tests with a real DB would have caught it immediately.

## Testcontainers — Real Postgres in Tests

Testcontainers is a library that starts Docker containers in your test JVM:

```java
// test/integration/AbstractIntegrationTest.java
static final PostgreSQLContainer<?> postgres;

static {
    postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("taskflow_test")
        .withUsername("taskflow")
        .withPassword("taskflow_test");
    postgres.start();  // starts a real Postgres 16 container
}

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    // Tell Spring to use the container's JDBC URL
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

`@DynamicPropertySource` overrides Spring properties at test startup, AFTER the container is running. Without it, `application.yml` would point to `localhost:5432` which doesn't exist in CI.

## The Singleton Pattern — A Bug We Fixed

The original code had:

```java
@Testcontainers  // ← on the abstract base class
@SpringBootTest
public abstract class AbstractIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(...)...;
}
```

This caused a hard-to-diagnose bug:

1. `AuthControllerIntegrationTest` runs → Testcontainers starts the container (say, port 54321)
2. Spring context is cached with JDBC URL `jdbc:postgresql://localhost:54321/taskflow_test`
3. Auth tests finish → **Testcontainers STOPS the container** (because `@Testcontainers` manages its lifecycle per class)
4. `TeamControllerIntegrationTest` runs → Testcontainers starts a NEW container (port 54322)
5. Spring context is REUSED (cached) — still has the OLD URL `localhost:54321`
6. Connection pool tries to connect → `Connection to localhost:54321 refused`
7. HikariCP waits 20 seconds → throws → JWT filter catches it → authentication fails → **401 on every request**

The bug manifested as "team tests get 401" but the actual cause was Postgres being unreachable.

**The fix**: static initializer instead of `@Testcontainers`/`@Container`:

```java
// No @Testcontainers annotation
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {  // runs once when the class is first loaded by the JVM
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")...;
        postgres.start();
    }
    // ...
}
```

`static {}` is a Java static initializer. It runs exactly once when `AbstractIntegrationTest` is first classloaded. No JUnit extension manages its lifecycle — it stays running for the entire JVM lifetime. The Spring context is cached once and uses the same URL throughout all test classes.

## `HttpRetryException` — The Client-Side Bug

Auth tests that expected `401` responses were throwing `ResourceAccessException`:
```
cannot retry due to server authentication, in streaming mode
```

This is Java's `HttpURLConnection` behavior: when you POST a body with `Content-Length` set in advance (`streaming mode`), and the server returns 401, the Java client throws `HttpRetryException` because it already sent the body and can't re-send it for authentication retry.

**The attempted fix**: `SimpleClientHttpRequestFactory.setOutputStreaming(false)`

This was deprecated in Spring 6.1 and no longer effective in Spring 6.1.13.

**The actual fix**: Use Apache HttpClient 5:

```java
// build.gradle.kts
testImplementation("org.apache.httpcomponents.client5:httpclient5")
```

```java
// AbstractIntegrationTest.java
@BeforeEach
void configureHttpClient() {
    restTemplate.getRestTemplate().setRequestFactory(
        new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault())
    );
}
```

Apache HttpClient 5 uses its own HTTP stack (not `HttpURLConnection`) and handles 401 responses gracefully — it reads the response body and returns the status code, no exception thrown.

Why `@BeforeEach` and not a one-time setup? Because `TestRestTemplate` is a shared Spring bean, and setting the factory once for the class might be overridden by other test classes sharing the same context. Setting it before EACH test guarantees it's always configured correctly.

## `TestRestTemplate` vs `MockMvc`

Spring offers two testing approaches:

| | `MockMvc` | `TestRestTemplate` |
|-|-----------|-------------------|
| Boot a real server? | No — simulates HTTP | Yes — `RANDOM_PORT` |
| Tests the full stack? | Almost — skips Tomcat | Fully — Tomcat + filters |
| Speed | Fast | Slower |
| Best for | Unit-level controller tests | Full integration tests |

This project uses `TestRestTemplate` because:
- We want to test the full filter chain (JWT authentication, CORS, etc.)
- `MockMvc` with `@WebMvcTest` skips JPA, so it can't test real database behavior
- We want to catch integration bugs — the whole point

## Key Files

| File | What to study |
|------|---------------|
| `test/integration/AbstractIntegrationTest.java` | Singleton pattern, HttpClient5 setup |
| `test/integration/AuthControllerIntegrationTest.java` | Testing auth endpoints, 401 scenarios |
| `test/integration/TeamControllerIntegrationTest.java` | Testing authenticated endpoints |
| `test/resources/application-test.yml` | Test-specific config overrides |
| `build.gradle.kts` | Testcontainers + httpclient5 dependencies |

## How to Add a New Integration Test

```java
class MyNewFeatureIntegrationTest extends AbstractIntegrationTest {

    @Test
    void myScenario() {
        // 1. Register a user and get a token
        String token = registerAndGetToken(uniqueEmail(), "SecurePass123!", "Test User");

        // 2. Make authenticated requests
        ResponseEntity<MyResponse> response = restTemplate.exchange(
            "/api/v1/my-endpoint",
            HttpMethod.POST,
            authRequest(requestBody, token),  // helper from AbstractIntegrationTest
            MyResponse.class
        );

        // 3. Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
    }
}
```

`uniqueEmail()` generates a UUID-based email — ensures tests don't conflict even when running multiple test classes against the same shared database.
