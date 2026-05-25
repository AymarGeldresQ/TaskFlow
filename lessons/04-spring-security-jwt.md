# Lesson 04 — Spring Security + JWT

## The Concept

Every request to a protected endpoint goes through a **filter chain** — a sequence of filters that run before your controller ever sees the request. Spring Security adds its own filters to this chain.

The flow for a protected request:

```
HTTP Request
    │
    ▼
[CorsFilter]
    │
    ▼
[SecurityContextHolderFilter]  ← sets up the security context for this request
    │
    ▼
[JwtAuthenticationFilter]      ← OUR filter — extracts and validates the JWT
    │
    ▼
[AuthorizationFilter]          ← checks: is there an authenticated principal?
    │                               if NO → 401 Unauthorized
    │                               if YES → continue
    ▼
[DispatcherServlet]
    │
    ▼
[Controller]
```

The JWT filter's job: read the `Authorization: Bearer <token>` header, validate the token, and if valid, set the authenticated user in the `SecurityContextHolder`. From that point, every downstream component (including `AuthorizationFilter`) can ask "who is this?" and get an answer.

## In This Project

### The JWT Filter

```java
// infrastructure/security/JwtAuthenticationFilter.java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String authHeader = request.getHeader("Authorization");

        // 1. No Bearer header? Skip. Pass to next filter.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // strip "Bearer "

        try {
            // 2. Extract email from JWT payload
            String email = jwtService.extractEmail(token);

            // 3. Only proceed if no auth already set (prevents double-processing)
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 4. Load user from DB — this is what verifies the user still exists
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                // 5. Validate: signature OK? not expired? email matches?
                if (jwtService.isAccessTokenValid(token, userDetails)) {
                    // 6. Create auth token and SET it in the security context
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            // 7. Any failure (invalid signature, expired, user deleted) → skip silently
            log.debug("JWT authentication failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }

        // 8. ALWAYS continue the filter chain, even if auth failed
        filterChain.doFilter(request, response);
    }
}
```

Step 4 is important: even though the JWT contains the user's email, we still load the user from the database. Why? Because the user might have been deleted or deactivated AFTER the token was issued. The JWT itself is valid, but the account doesn't exist anymore. The DB lookup catches this.

### The JWT Service

```java
// infrastructure/security/JwtService.java

// Generating an access token
public String generateAccessToken(UserDetails userDetails) {
    return Jwts.builder()
        .subject(userDetails.getUsername())  // email stored here
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
        .signWith(signingKey)  // HMAC-SHA256 signature
        .compact();
}

// Validating a token
public boolean isAccessTokenValid(String token, UserDetails userDetails) {
    try {
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    } catch (JwtException | IllegalArgumentException e) {
        return false;  // tampered, malformed, or wrong key → false
    }
}
```

The `signingKey` is derived from `JWT_SECRET` (an environment variable). If someone tampers with the token payload (e.g., changes the email to admin@company.com), the signature won't match the new payload and `isAccessTokenValid` returns false.

### Security Configuration

```java
// infrastructure/security/SecurityConfig.java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) {
    JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    
    return http
        .csrf(AbstractHttpConfigurer::disable)           // REST APIs don't need CSRF tokens
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // no sessions
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))  // 401 not 403
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()  // register/login don't need a token
            .anyRequest().authenticated()                    // everything else requires auth
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)  // add our filter
        .build();
}
```

`SessionCreationPolicy.STATELESS` tells Spring Security to never create an HTTP session. Each request must bring its own token. This is the essence of stateless authentication.

## What a JWT Looks Like

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZUBleGFtcGxlLmNvbSIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDAwOTAwfQ.SIGNATURE
│                   │ │                                                                                                │ │         │
└── header (base64) ┘ └─────────────────────── payload (base64) ────────────────────────────────────────────────────┘ └─ HMAC ──┘
```

Decode the middle part (payload):
```json
{
  "sub": "alice@example.com",
  "iat": 1700000000,
  "exp": 1700000900
}
```

The signature covers header + payload. If you change ANYTHING in the payload, the signature becomes invalid. The server verifies the signature with the same secret key — if it doesn't match, the token is rejected.

**Tokens are NOT encrypted.** Anyone can base64-decode the payload and read the email. Don't put passwords or sensitive data in JWT claims. The security comes from the signature, not the encoding.

## What Breaks Without It

Without `SessionCreationPolicy.STATELESS`:
- Spring creates an HTTP session and stores auth in it
- Your API returns `Set-Cookie: JSESSIONID=...` headers
- Requests without the cookie get redirected to a login page — broken for REST clients
- Horizontal scaling breaks because different servers have different sessions

Without `exceptionHandling + HttpStatusEntryPoint`:
- Unauthenticated requests get `403 Forbidden` instead of `401 Unauthorized`
- That's technically wrong: 401 means "you need to authenticate", 403 means "you're authenticated but not allowed"

## Key Files

| File | Role |
|------|------|
| `infrastructure/security/JwtAuthenticationFilter.java` | Reads Bearer token, sets authentication |
| `infrastructure/security/JwtService.java` | Generates and validates JWTs |
| `infrastructure/security/SecurityConfig.java` | Configures the filter chain |
| `infrastructure/security/UserDetailsServiceImpl.java` | Loads user from DB by email |
| `infrastructure/security/JwtUserDetails.java` | Wraps user data for Spring Security |
| `infrastructure/config/JwtProperties.java` | Binds `jwt.*` config from application.yml |
