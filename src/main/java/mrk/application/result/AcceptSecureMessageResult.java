package mrk.application.result;

public class AcceptSecureMessageResult {
    private final long inboundMessageId;
    private final String requestId;
    private final boolean duplicate;

    public AcceptSecureMessageResult(long inboundMessageId, String requestId, boolean duplicate) {
        this.inboundMessageId = inboundMessageId;
        this.requestId = requestId;
        this.duplicate = duplicate;
    }

    public long getInboundMessageId() {
        return inboundMessageId;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isDuplicate() {
        return duplicate;
    }
}