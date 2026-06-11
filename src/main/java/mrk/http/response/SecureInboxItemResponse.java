package mrk.http.response;

import java.time.LocalDateTime;

public class SecureInboxItemResponse {
    private final long messageId;
    private final long senderId;
    private final String status;
    private final boolean oneTime;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    public SecureInboxItemResponse(
            long messageId,
            long senderId,
            String status,
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

    public String getStatus() {
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