package mrk.infrastructure.event.payload;

public class SecureMessageEncryptedPayload {
    private long secureMessageId;
    private long recipientId;

    public SecureMessageEncryptedPayload() {
    }

    public long getSecureMessageId() {
        return secureMessageId;
    }

    public void setSecureMessageId(long secureMessageId) {
        this.secureMessageId = secureMessageId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(long recipientId) {
        this.recipientId = recipientId;
    }
}