package mrk.http.response;

public class AcceptedMessageResponse {
    private final String status;
    private final long inboundMessageId;
    private final String requestId;
    private final boolean duplicate;

    public AcceptedMessageResponse(
            String status,
            long inboundMessageId,
            String requestId,
            boolean duplicate
    ) {
        this.status = status;
        this.inboundMessageId = inboundMessageId;
        this.requestId = requestId;
        this.duplicate = duplicate;
    }

    public String getStatus() {
        return status;
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