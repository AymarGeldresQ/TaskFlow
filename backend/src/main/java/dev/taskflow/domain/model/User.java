package dev.taskflow.domain.model;

import dev.taskflow.domain.exception.DomainException;
import java.time.Instant;
import java.util.UUID;

public final class User {

    private UUID id;
    private String email;
    private String passwordHash;
    private String fullName;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private User() {}

    public static User create(String email, String passwordHash, String fullName) {
        if (email == null || email.isBlank()) {
            throw new DomainException("User email cannot be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("User password hash cannot be blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("User full name cannot be blank");
        }
        User user = new User();
        user.id = UUID.randomUUID();
        user.email = email.toLowerCase().trim();
        user.passwordHash = passwordHash;
        user.fullName = fullName.trim();
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        return user;
    }

    public static User reconstitute(UUID id, String email, String passwordHash, String fullName,
                                    String avatarUrl, Instant createdAt, Instant updatedAt, Instant deletedAt) {
        User user = new User();
        user.id = id;
        user.email = email;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        user.avatarUrl = avatarUrl;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        user.deletedAt = deletedAt;
        return user;
    }

    public void updateProfile(String fullName, String avatarUrl) {
        if (fullName != null && !fullName.isBlank()) {
            this.fullName = fullName.trim();
        }
        this.avatarUrl = avatarUrl;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
