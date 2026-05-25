# Lesson 05 — The Spring Boot Auto-Configuration Trap

## This Is a Real Bug We Fixed

During development of this project, ALL authenticated requests returned 401. The JWT filter was running, the token was valid, the user was loaded — but the security context was being cleared. Here's exactly what happened and why.

## Spring Boot Auto-Configures Filters

Spring Boot has a feature called **auto-configuration**: if it sees a bean of a certain type, it automatically registers it in the servlet container. Any class that implements `Filter` and is annotated with `@Component` gets auto-registered as a servlet filter.

The original code had this:

```java
@Component  // ← THIS WAS THE BUG
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // ...
}
```

AND in `SecurityConfig`:

```java
@Bean
public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
    return new JwtAuthenticationFilter(jwtService, userDetailsService);
}

// and in securityFilterChain:
.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
```

Result: the filter was registered **TWICE**.

## The Two-Filter Problem

The servlet container sees TWO separate registrations:
1. One from `@Component` auto-registration — runs as a plain servlet filter
2. One from `addFilterBefore()` inside the Spring Security chain

The execution order was:

```
HTTP Request
    │
    ▼
[Standalone JwtAuthenticationFilter]  ← auto-registered, runs FIRST
    │  sets SecurityContextHolder.getContext().setAuthentication(auth)
    │
    ▼
[SecurityContextHolderFilter]  ← Spring Security 6 sets up a FRESH deferred context
    │  → REPLACES the security context set above with an empty one
    │
    ▼
[JwtAuthenticationFilter AGAIN]  ← OncePerRequestFilter sees request already processed
    │  → SKIPS because of the "already ran" flag it sets on the request
    │
    ▼
[AuthorizationFilter]
    │  authentication == null → 401
```

The `SecurityContextHolderFilter` in Spring Security 6 uses a **deferred security context** — it sets up the context lazily. When the JWT filter runs BEFORE it (as a standalone servlet filter), the auth token is set in a context that `SecurityContextHolderFilter` then REPLACES with an empty one.

Then `OncePerRequestFilter` (which `JwtAuthenticationFilter` extends) saw the request-attribute flag set by the first run and skipped the second run in the security chain. Net result: no authentication ever survived to `AuthorizationFilter`.

## The Fix

Remove the filter from Spring's bean detection entirely. Don't use `@Component`. Don't declare it as a `@Bean`. Create it as a local variable inside the `@Bean` method:

```java
// infrastructure/security/SecurityConfig.java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) {
    // Created as a LOCAL VARIABLE — NOT a Spring bean
    // Spring Boot only auto-registers Filter instances that are Spring beans
    JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    
    return http
        // ...
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

The filter is registered ONCE — inside the Spring Security chain — and Spring Boot never sees it as a standalone filter.

## The Rule to Remember

> **If a `Filter` class is registered inside `addFilterBefore()` in a `SecurityFilterChain`, do NOT make it a `@Component` or a `@Bean`. It must be created locally inside the security config.**

If you use `@Bean` in `SecurityConfig` to create the filter, Spring Boot still auto-registers it as a servlet filter. The only safe way is a local variable (not a bean at all).

## Diagnosing This Bug Yourself

Signs of double-filter registration:
- JWT filter logs confirm "auth set" for a request
- But the request still gets 401
- Spring Security DEBUG logs show the security context being cleared

Enable Spring Security debug in tests:
```yaml
# application-test.yml
logging:
  level:
    org.springframework.security: DEBUG
```

Then look for `SecurityContextHolderFilter` in the logs — you'll see it creating a new context AFTER your filter set one.

## What `OncePerRequestFilter` Actually Does

`OncePerRequestFilter` is a Spring helper class that ensures a filter runs only once per request — even if there are forwards or includes (where the servlet container can re-invoke filters).

It works by setting a request attribute:
```java
// simplified Spring source
String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
if (request.getAttribute(alreadyFilteredAttributeName) != null) {
    filterChain.doFilter(request, response);  // skip — already ran
    return;
}
request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
// ... run the actual filter
```

In the double-registration bug, the standalone run sets this flag. The security-chain run sees the flag and skips. So only the standalone run executes — and that one's result gets wiped by `SecurityContextHolderFilter`.

## Key Files

| File | What Changed |
|------|-------------|
| `infrastructure/security/JwtAuthenticationFilter.java` | Removed `@Component` |
| `infrastructure/security/SecurityConfig.java` | Filter created as local var, not `@Bean` |
