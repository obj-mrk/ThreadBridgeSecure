package mrk.application.result;

import mrk.domain.value.SecureMessageStatus;

import java.time.LocalDateTime;

public class SecureInboxItem {
    private final long messageId;
    private final long senderId;
    private final SecureMessageStatus status;
    private final boolean oneTime;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    public SecureInboxItem(
            long messageId,
            long senderId,
            SecureMessageStatus status,
            boolean oneTime,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.status = status;
        this.oneTime = oneTime;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getSenderId() {
        return senderId;
    }

    public SecureMessageStatus getStatus() {
        return status;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}