package mrk.domain.model;

import mrk.domain.value.UserStatus;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;
    private LocalDateTime createdAt;
    private UserStatus status;

    public User() {
    }

    public User(Long id, String username, LocalDateTime createdAt, UserStatus status) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
        this.status = status;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return id;
    }

    public void setUserId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setName(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}