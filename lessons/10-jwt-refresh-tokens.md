# Lesson 10 — JWT vs Opaque Refresh Tokens

## Two Token Types

This project uses two different token strategies for two different purposes:

| Token | Type | Format | Where stored |
|-------|------|--------|--------------|
| Access token | JWT | Signed JSON | Client memory only — NEVER DB |
| Refresh token | Opaque | UUID (hashed) | Database |

This distinction is not arbitrary — each type is chosen to optimize for its specific use case.

## Access Token — JWT

```java
// infrastructure/security/JwtService.java
public String generateAccessToken(UserDetails userDetails) {
    return Jwts.builder()
        .subject(userDetails.getUsername())  // email
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))  // 15 minutes
        .signWith(signingKey)  // HMAC-SHA256
        .compact();
}
```

Access tokens expire in 15 minutes (`JWT_ACCESS_EXPIRATION_MS: 900000`). Why so short?

JWTs are **stateless** — the server doesn't store them. Verification only requires checking the signature and expiration date. This is fast: no database query on every authenticated request (except the user lookup in `UserDetailsServiceImpl`).

The tradeoff: **you can't revoke a JWT before it expires**. If a user logs out, their access token is still technically valid until expiration. This is acceptable for 15 minutes — the window of risk is small.

## Refresh Token — Opaque UUID

```java
// infrastructure/security/JwtService.java
public String generateOpaqueRefreshToken() {
    return UUID.randomUUID().toString();  // just a random string
}

public String hashToken(String rawToken) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hash);  // hex string of SHA-256 hash
}
```

The refresh token is a random UUID. It lasts 7 days.

**What's stored in the database**: the SHA-256 hash of the token, not the token itself.

```java
// application/usecase/auth/LoginUseCase.java
String rawRefreshToken = jwtService.generateOpaqueRefreshToken();  // UUID — sent to client
RefreshToken refreshToken = RefreshToken.create(
    user.getId(),
    jwtService.hashToken(rawRefreshToken),  // SHA-256 hash — stored in DB
    jwtService.refreshTokenExpiresAt()
);
refreshTokenRepository.save(refreshToken);
```

When the client sends the refresh token:
```java
// application/usecase/auth/RefreshTokenUseCase.java (simplified)
String tokenHash = jwtService.hashToken(request.refreshToken());  // hash what came in
RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
    .orElseThrow(() -> new InvalidRefreshTokenException());  // compare hashes
```

Why hash it? If the database is compromised, the attacker gets hashed tokens — not the actual tokens. They can't use a SHA-256 hash to authenticate (they'd need the original UUID to send in a request).

## Token Rotation

```java
// application/usecase/auth/RefreshTokenUseCase.java (simplified)
public AuthResponse execute(RefreshRequest request) {
    // 1. Find and validate the token
    String tokenHash = jwtService.hashToken(request.refreshToken());
    RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
        .filter(t -> !t.isExpired())
        .orElseThrow(() -> new InvalidRefreshTokenException());

    // 2. DELETE the old token — it's now used
    refreshTokenRepository.delete(stored);

    // 3. Issue a completely new refresh token
    String newRawRefreshToken = jwtService.generateOpaqueRefreshToken();
    RefreshToken newRefreshToken = RefreshToken.create(
        stored.getUserId(),
        jwtService.hashToken(newRawRefreshToken),
        jwtService.refreshTokenExpiresAt()
    );
    refreshTokenRepository.save(newRefreshToken);

    // 4. Issue a new access token
    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
    String newAccessToken = jwtService.generateAccessToken(userDetails);

    return AuthResponse.of(newAccessToken, newRawRefreshToken, ...);
}
```

**Token rotation** means: every time you use a refresh token, it gets deleted and a new one is issued. The old token becomes invalid immediately.

This means if an attacker steals a refresh token and uses it, the legitimate user's next refresh will fail (`InvalidRefreshTokenException`). The system detects the conflict — both parties tried to use the same token.

Some systems implement "refresh token families" to detect and revoke all tokens for a user when reuse is detected. This project currently returns 401 on reuse, which is sufficient for most use cases.

## Logout — Revoke All

```java
// application/usecase/auth/LogoutUseCase.java (simplified)
public void execute(UUID userId) {
    refreshTokenRepository.deleteAllByUserId(userId);  // revoke ALL refresh tokens for this user
}
```

On logout, all refresh tokens are deleted. The current access token stays valid until it expires (15 minutes max). This is the accepted tradeoff for stateless auth. If you need instant revocation, you'd add an access token blocklist (stored in Redis), but that adds infrastructure complexity.

## The Complete Auth Flow

```
1. POST /auth/register or /auth/login
   Server: generates accessToken (JWT, 15min) + refreshToken (UUID, 7 days, hashed in DB)
   Client: stores accessToken in memory, refreshToken in secure storage

2. Every API request:
   Client: sends Authorization: Bearer <accessToken>
   Server: validates JWT signature + expiry (no DB query)

3. When accessToken expires (15 min later):
   Client: sends refreshToken to POST /auth/refresh
   Server: finds hash in DB, deletes old token, creates new refresh token + new access token
   Client: stores new tokens

4. Logout:
   Client: sends accessToken to POST /auth/logout
   Server: deletes all refresh tokens for this user
   Client: discards both tokens from memory
```

## Security Checklist for JWTs

| Requirement | This Project |
|-------------|-------------|
| Short-lived access tokens | ✅ 15 minutes |
| Signed (tamper-proof) | ✅ HMAC-SHA256 |
| Not encrypted (don't put secrets in claims) | ✅ Only email in claims |
| Long-lived tokens stored server-side | ✅ Opaque refresh tokens in DB |
| Stored tokens hashed, not plain | ✅ SHA-256 |
| Token rotation on refresh | ✅ Old token deleted on use |
| Logout revokes all sessions | ✅ Delete all tokens for user |
| Secret from environment (not hardcoded) | ✅ `JWT_SECRET` env var |

## Key Files

| File | What to study |
|------|---------------|
| `infrastructure/security/JwtService.java` | JWT generation, token hashing |
| `domain/model/RefreshToken.java` | Refresh token domain model |
| `application/usecase/auth/LoginUseCase.java` | Token creation on login |
| `application/usecase/auth/RefreshTokenUseCase.java` | Token rotation |
| `application/usecase/auth/LogoutUseCase.java` | Session revocation |
| `infrastructure/persistence/entity/RefreshTokenEntity.java` | What's stored in the DB |
