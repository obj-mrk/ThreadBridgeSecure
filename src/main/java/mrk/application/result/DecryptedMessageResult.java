package mrk.application.result;

public class DecryptedMessageResult {
    private final long messageId;
    private final long senderId;
    private final String text;
    private final boolean destroyedAfterRead;

    public DecryptedMessageResult(
            long messageId,
            long senderId,
            String text,
            boolean destroyedAfterRead
    ) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.destroyedAfterRead = destroyedAfterRead;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getSenderId() {
        return senderId;
    }

    public String getText() {
        return text;
    }

    public boolean isDestroyedAfterRead() {
        return destroyedAfterRead;
    }
}