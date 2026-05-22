package mrk.application.command;

public class AcceptSecureMessageCommand {
    private final String requestId;
    private final long senderId;
    private final long recipientId;
    private final String text;
    private final int ttlSeconds;
    private final boolean oneTime;

    public AcceptSecureMessageCommand(
            String requestId,
            long senderId,
            long recipientId,
            String text,
            int ttlSeconds,
            boolean oneTime
    ) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.text = text;
        this.ttlSeconds = ttlSeconds;
        this.oneTime = oneTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public String getText() {
        return text;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public boolean isOneTime() {
        return oneTime;
    }
}