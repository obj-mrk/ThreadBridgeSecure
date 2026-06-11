package mrk.domain.model;

import mrk.domain.value.NotificationStatus;
import mrk.domain.value.NotificationType;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private long userId;
    private long secureMessageId;
    private NotificationType type;
    private NotificationStatus status;
    private String text;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static Notification secureMessageAvailable(long recipientId, long secureMessageId) {
        Notification notification = new Notification();
        notification.setUserId(recipientId);
        notification.setSecureMessageId(secureMessageId);
        notification.setType(NotificationType.SECURE_MESSAGE_AVAILABLE);
        notification.setStatus(NotificationStatus.NEW);
        notification.setText("New secure message is available");
        return notification;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getSecureMessageId() {
        return secureMessageId;
    }

    public void setSecureMessageId(long secureMessageId) {
        this.secureMessageId = secureMessageId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}