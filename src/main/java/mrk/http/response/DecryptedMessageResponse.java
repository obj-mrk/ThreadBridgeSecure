package mrk.http.response;

public class DecryptedMessageResponse {
    private final String status;
    private final long messageId;
    private final long senderId;
    private final String text;
    private final boolean destroyedAfterRead;

    public DecryptedMessageResponse(
            String status,
            long messageId,
            long senderId,
            String text,
            boolean destroyedAfterRead
    ) {
        this.status = status;
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.destroyedAfterRead = destroyedAfterRead;
    }

    public String getStatus() {
        return status;
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