package mrk.application.command;

public class DecryptSecureMessageCommand {
    private final long messageId;
    private final long recipientId;

    public DecryptSecureMessageCommand(long messageId, long recipientId) {
        if (messageId <= 0) {
            throw new IllegalArgumentException("Message id must be positive");
        }

        if (recipientId <= 0) {
            throw new IllegalArgumentException("Recipient id must be positive");
        }

        this.messageId = messageId;
        this.recipientId = recipientId;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getRecipientId() {
        return recipientId;
    }
}