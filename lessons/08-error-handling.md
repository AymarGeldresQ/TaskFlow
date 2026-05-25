# Lesson 08 — Error Handling Strategy

## The Concept

Good API error handling has two goals:
1. Give clients enough information to fix their problem
2. Don't leak internal details that attackers can exploit

Every HTTP status code has a specific meaning. Using the wrong one misleads clients and breaks tooling.

## In This Project

### `GlobalExceptionHandler` — One Place for All Errors

```java
// infrastructure/rest/advice/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex) {
        return ResponseEntity.status(422)  // Unprocessable Entity
            .body(ApiError.of("DOMAIN_RULE_VIOLATION", ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(ApiError.of(ex.getEntityType().toUpperCase() + "_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<ApiError> handleEmailTaken(EmailAlreadyTakenException ex) {
        return ResponseEntity.status(409)  // Conflict
            .body(ApiError.of("EMAIL_ALREADY_TAKEN", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTaskTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidTransition(InvalidTaskTransitionException ex) {
        return ResponseEntity.status(422)
            .body(ApiError.of("INVALID_TASK_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedOperationException ex) {
        return ResponseEntity.status(403)  // Forbidden
            .body(ApiError.of("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler({InvalidRefreshTokenException.class, BadCredentialsException.class})
    public ResponseEntity<ApiError> handleAuthError(RuntimeException ex) {
        return ResponseEntity.status(401)
            .body(ApiError.of("AUTHENTICATION_FAILED", "Invalid credentials or token"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        // Converts Bean Validation errors to structured field errors
        return ResponseEntity.status(400).body(...);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);  // log with stack trace
        return ResponseEntity.status(500)
            .body(ApiError.of("INTERNAL_ERROR", "An unexpected error occurred"));  // NO details to client
    }
}
```

`@RestControllerAdvice` means: "this class handles exceptions thrown by any `@RestController` in the app." You throw anywhere in the call stack, this catches it.

### Status Code Map

| Exception | HTTP Status | Reason |
|-----------|-------------|--------|
| `DomainException` | 422 | Valid syntax, but violates a business rule |
| `EntityNotFoundException` | 404 | Resource doesn't exist |
| `EmailAlreadyTakenException` | 409 | Conflict with existing state |
| `InvalidTaskTransitionException` | 422 | Business rule violation |
| `UnauthorizedOperationException` | 403 | Authenticated, but not allowed |
| `BadCredentialsException` | 401 | Wrong credentials |
| `MethodArgumentNotValidException` | 400 | Invalid request syntax/structure |
| `Exception` (catch-all) | 500 | Unexpected — log it, hide details |

### 422 vs 400 — An Important Distinction

- **400 Bad Request**: the request itself is malformed — missing a required field, invalid email format, wrong type. Bean Validation catches these.
- **422 Unprocessable Entity**: the request is syntactically valid but violates business logic.

Example:
```
POST /api/v1/tasks/{id}/status
{"status": "BACKLOG"}
```
If the task is currently `DONE`, this is a valid JSON request with a valid status value — but it violates the state machine. That's 422, not 400.

### Why Wrong Password Returns 401, Not 404

A subtlety in `LoginUseCase`:

```java
// application/usecase/auth/LoginUseCase.java
User user = userRepository.findByEmail(request.email())
    .filter(User::isActive)
    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));  // NOT EntityNotFoundException

if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
    throw new BadCredentialsException("Invalid credentials");
}
```

Both "user not found" and "wrong password" throw the same exception with the same message. Why?

**User enumeration attack**: if "email not found" returns 404 and "wrong password" returns 401, an attacker can test whether any email address has an account. They'd try `attacker@gmail.com` — if they get 404, no account. If they get 401, there IS an account. Now they know whom to target.

By returning 401 for both cases, you give away nothing. The attacker can't distinguish "this email doesn't exist" from "this email exists but wrong password."

### The Error Response Shape

```java
// infrastructure/rest/advice/ApiError.java
public record ApiError(
    String code,      // machine-readable, for client logic: "INVALID_TASK_TRANSITION"
    String message,   // human-readable, for developers: "Cannot transition from DONE to BACKLOG"
    List<FieldError> fieldErrors  // only for 400 validation errors
) { }
```

The `code` field lets frontend clients handle specific errors programmatically:
```javascript
if (error.code === "EMAIL_ALREADY_TAKEN") {
    showError("This email is already registered");
} else if (error.code === "INVALID_TASK_TRANSITION") {
    showError("You can't move this task to that status");
}
```

### 500 Hides the Stack Trace — Intentionally

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);  // full stack trace in logs
    return ResponseEntity.status(500)
        .body(ApiError.of("INTERNAL_ERROR", "An unexpected error occurred"));  // nothing specific
}
```

The stack trace is logged server-side (where you can see it). The client gets a generic message. Stack traces contain class names, file paths, library versions — information attackers use to find known vulnerabilities.

In `application.yml`:
```yaml
server:
  error:
    include-stacktrace: never     # Spring's default error page — also no trace
    include-message: never
    include-binding-errors: never
```

## What Breaks Without It

Without `@RestControllerAdvice`:
- Unhandled exceptions propagate to Spring's default error handler
- Spring returns its own JSON error with a stack trace in some modes
- Different parts of your app return different error shapes — clients can't rely on a consistent format

Without the auth-error consolidation (returning 401 for both "not found" and "wrong password"):
- You're leaking account existence information
- Security scanners will flag this as an information disclosure vulnerability

## Key Files

| File | What to study |
|------|---------------|
| `infrastructure/rest/advice/GlobalExceptionHandler.java` | Full exception-to-status mapping |
| `infrastructure/rest/advice/ApiError.java` | Error response structure |
| `domain/exception/DomainException.java` | Base for business rule violations |
| `domain/exception/InvalidTaskTransitionException.java` | Typed exception with context |
| `application/usecase/auth/LoginUseCase.java` | User enumeration prevention |
