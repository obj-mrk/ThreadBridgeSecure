package mrk.application.result;

public class ProcessSecureMessageResult {
    private final long inboundMessageId;
    private final long secureMessageId;

    public ProcessSecureMessageResult(long inboundMessageId, long secureMessageId) {
        this.inboundMessageId = inboundMessageId;
        this.secureMessageId = secureMessageId;
    }

    public long getInboundMessageId() {
        return inboundMessageId;
    }

    public long getSecureMessageId() {
        return secureMessageId;
    }
}