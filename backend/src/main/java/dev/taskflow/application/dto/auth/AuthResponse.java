package dev.taskflow.application.dto.auth;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserInfo user
) {

    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresIn, UUID userId, String email, String fullName) {
        return new AuthResponse(
            accessToken,
            refreshToken,
            "Bearer",
            expiresIn,
            new UserInfo(userId, email, fullName)
        );
    }

    public record UserInfo(UUID id, String email, String fullName) {}
}
