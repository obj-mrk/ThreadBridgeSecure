package mrk.domain.model;

import mrk.domain.value.UserStatus;

import java.time.LocalDateTime;

public class User {
    private final Long id;
    private final String username;
    private final LocalDateTime createdAt;
    private final UserStatus status;

    private User(Long id, String username, LocalDateTime createdAt, UserStatus status) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static User register(String username, LocalDateTime now) {
        validateUsername(username);

        if (now == null) {
            throw new IllegalArgumentException("Registration time must not be null");
        }

        return new User(null, username.trim(), now, UserStatus.ACTIVE);
    }

    public static User restore(Long id, String username, LocalDateTime createdAt, UserStatus status) {
        validateId(id);
        validateUsername(username);

        if (createdAt == null) {
            throw new IllegalArgumentException("Created at must not be null");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }

        return new User(id, username.trim(), createdAt, status);
    }

    private static void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
    }

    private static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }

        if (username.trim().length() > 100) {
            throw new IllegalArgumentException("Username must not be longer than 100 characters");
        }
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserStatus getStatus() {
        return status;
    }
}