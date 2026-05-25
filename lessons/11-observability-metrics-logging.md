# Lesson 11 — Observability: Metrics, Structured Logging, and Correlation IDs

## The Concept

Observability answers the question "what is the system doing right now?" without needing to redeploy or add logging after the fact. Three pillars:

| Pillar | Tool | Purpose |
|--------|------|---------|
| Metrics | Micrometer + Prometheus | Numeric measurements over time (counts, gauges, timers) |
| Logs | Logback + Logstash encoder | Human/machine-readable event records |
| Traces | Correlation ID (MDC) | Thread-local ID that ties all log lines for one request together |

This project implements all three. The key insight: **observability is infrastructure concern**, so everything lives in `infrastructure/config/` — zero coupling to domain or application layers.

## In This Project

### Metrics — Micrometer `MeterBinder`

```java
// infrastructure/config/TaskMetrics.java
@Component
public class TaskMetrics implements MeterBinder {

    private final TaskJpaRepository taskJpaRepository;

    public TaskMetrics(TaskJpaRepository taskJpaRepository) {
        this.taskJpaRepository = taskJpaRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("taskflow.tasks.active", taskJpaRepository,
                repo -> repo.countByStatusNotInAndDeletedAtIsNull(
                    List.of(TaskStatus.DONE, TaskStatus.CANCELLED)))
            .description("Number of active (non-terminal) tasks across all projects")
            .register(registry);
    }
}
```

**Why `MeterBinder`?** Spring Boot auto-discovers any bean that implements `MeterBinder` and calls `bindTo()` once at startup. The `Gauge` is then registered — but it doesn't run the lambda yet.

**The lambda runs at scrape time.** When Prometheus scrapes `/actuator/prometheus`, Micrometer calls the supplier to get the current value. This is a pull model — one DB query per scrape interval (typically 15-60s), not per request. That's the right tradeoff for a gauge.

**Why not `@Counted` or `@Timed` on the use case?** Those annotations count method calls and measure execution time. A gauge measures a STATE at a point in time. Active tasks count isn't "how many CreateTask calls happened" — it's "how many tasks exist right now in a non-terminal state."

```
GET /actuator/prometheus
→ taskflow_tasks_active 17.0
```

### Correlation IDs — MDC

```java
// infrastructure/config/CorrelationIdFilter.java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(...) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();  // generate if client didn't send one
        }
        MDC.put(MDC_KEY, correlationId);            // put into thread-local MDC
        response.setHeader(CORRELATION_ID_HEADER, correlationId);  // echo back to client
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);  // CRITICAL: clean up so next request gets a fresh ID
        }
    }
}
```

MDC (Mapped Diagnostic Context) is a thread-local map that Logback automatically includes in every log line. Once you `put(key, value)`, every log statement on that thread for the rest of the request will include that key-value pair — without passing it as a parameter.

**Why `finally`?** Servlet containers reuse threads (from a thread pool). Without `MDC.remove()`, the next request on the same thread would inherit the previous request's correlation ID. Silent corruption, impossible to debug.

### Structured Logging — Profile-Aware

```xml
<!-- src/main/resources/logback-spring.xml -->

<!-- Dev: human-readable with correlation ID -->
<springProfile name="dev,default">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{correlationId}] - %msg%n</pattern>
        </encoder>
    </appender>
</springProfile>

<!-- Prod: JSON — every field is machine-parseable -->
<springProfile name="prod">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <providers>
                <mdc/>        <!-- includes correlationId automatically -->
                <context/>
                <logLevel/>
                <loggerName/>
                <message/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
</springProfile>
```

**`%X{correlationId}`** — pulls the MDC key into the log pattern. Dev logs look like:
```
10:23:45.123 [http-nio-8080-exec-1] INFO  d.t.application.usecase.task.CreateTaskUseCase [a1b2c3d4] - Task created: ...
```

In prod, `LogstashEncoder` produces JSON that a log aggregator (Datadog, ELK, CloudWatch) can index and query:
```json
{"@timestamp":"2024-01-15T10:23:45.123Z","level":"INFO","logger":"...","message":"Task created...","correlationId":"a1b2c3d4"}
```

### Actuator + Prometheus Wiring

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true  # /actuator/health/liveness + /actuator/health/readiness
  metrics:
    tags:
      application: ${spring.application.name}  # adds app=taskflow-backend to every metric
```

The `application` common tag means every metric scraped from this service will have `application="taskflow-backend"` in Prometheus. When you run multiple services, you can filter: `taskflow_tasks_active{application="taskflow-backend"}`.

### k6 Load Testing Baseline

```javascript
// infra/k6/baseline.js
export const options = {
  stages: [
    { duration: '30s', target: 10 },  // ramp up to 10 VUs
    { duration: '1m',  target: 10 },  // hold at 10 VUs
    { duration: '15s', target: 0  },  // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95th percentile under 500ms
    http_req_failed:   ['rate<0.01'],  // error rate under 1%
  },
};
```

The baseline establishes the performance contract: 95% of requests under 500ms with less than 1% errors at 10 concurrent users. Every run either confirms performance is stable or signals a regression.

Run: `k6 run infra/k6/baseline.js`

## What Breaks Without It

**Without correlation IDs**: A 500 error in production shows in logs as 10 log lines from different classes — but you can't tell which lines belong to the same request. You're searching for the bug in a sea of unconnected entries.

**Without MDC cleanup**: Thread pool reuse means request B gets request A's correlation ID. All logs from B appear to belong to A. You chase A thinking it failed, but A succeeded.

**Without structured logging in prod**: Log aggregators treat each line as a raw string. You can't query `level=ERROR AND correlationId=X`. You grep through gigabytes of text.

**Without metrics**: You know the system returned errors, but not how many tasks were active, how long operations take, or when the problem started. You're flying blind.

**Without a load baseline**: A refactor improves code quality but introduces an N+1 query. Without a k6 baseline, you ship it and discover the regression on prod traffic at 2am.

## The Rule to Remember

**Observability must cost nothing at request time** — correlation IDs are assigned once, MDC is thread-local, metrics are updated atomically. The cost is paid at scrape time (once per minute), not per request.

## Key Files

| File | What to study |
|------|---------------|
| `infrastructure/config/TaskMetrics.java` | `MeterBinder` pattern, gauge vs counter |
| `infrastructure/config/CorrelationIdFilter.java` | MDC lifecycle — put, propagate, cleanup |
| `src/main/resources/logback-spring.xml` | Profile-aware logging, `%X{}` MDC in pattern, `LogstashEncoder` |
| `src/main/resources/application.yml` | Actuator config, common metric tags |
| `infrastructure/persistence/repository/TaskJpaRepository.java` | `countByStatusNotInAndDeletedAtIsNull` — gauge data source |
| `infra/k6/baseline.js` | Load test structure, VU stages, thresholds |
