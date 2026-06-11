package mrk.application.result;

import java.time.LocalDateTime;

public class SecureMessageReadResult {

    private final long messageId;
    private final long senderId;
    private final String plaintext;
    private final boolean oneTime;
    private final LocalDateTime createdAt;

    public SecureMessageReadResult(
            long messageId,
            long senderId,
            String plaintext,
            boolean oneTime,
            LocalDateTime createdAt
    ) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.plaintext = plaintext;
        this.oneTime = oneTime;
        this.createdAt = createdAt;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getSenderId() {
        return senderId;
    }

    public String getPlaintext() {
        return plaintext;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}