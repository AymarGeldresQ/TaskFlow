# ADR-002: JWT with Refresh Token Rotation

**Status:** Accepted  
**Date:** 2026-05-23

## Context

The API needs stateless authentication. Access tokens must be short-lived to limit exposure
if intercepted. Users should not need to re-login frequently.

## Decision

- **Access token:** JWT, 15-minute expiry, signed with HS256 + secret (min 256-bit)
- **Refresh token:** opaque random UUID, 7-day expiry, stored hashed (SHA-256) in `refresh_tokens` table
- **Rotation:** on every refresh, old token is revoked and a new one is issued
- **Revocation:** refresh tokens can be explicitly revoked on logout; all user tokens can be
  revoked if account is compromised

## Why Not Long-Lived JWTs

Long-lived JWTs cannot be revoked without server-side state (blacklist), which defeats the
purpose of stateless JWTs. Short access + persistent refresh is the standard trade-off.

## Why Not Sessions

Sessions require shared state across instances, complicating horizontal scaling.

## Consequences

**Positive:**
- Access token exposure window limited to 15 minutes
- Refresh token can be revoked (user logs out, token is invalidated immediately)
- Supports "logout from all devices" by revoking all user refresh tokens

**Negative:**
- DB lookup required on every refresh (acceptable: only every 15 minutes)
- Refresh token must be stored securely by the client (HttpOnly cookie recommended in prod)

## Security Notes

- JWT secret loaded from environment variable, never hardcoded
- Passwords hashed with BCrypt cost factor 12
- No sensitive fields (password_hash, token_hash) serialized in API responses
