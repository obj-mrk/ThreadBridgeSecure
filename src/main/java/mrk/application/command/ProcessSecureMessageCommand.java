package mrk.application.command;

public class ProcessSecureMessageCommand {
    private final long inboundMessageId;

    public ProcessSecureMessageCommand(long inboundMessageId) {
        if (inboundMessageId <= 0) {
            throw new IllegalArgumentException("Inbound message id must be positive");
        }

        this.inboundMessageId = inboundMessageId;
    }

    public long getInboundMessageId() {
        return inboundMessageId;
    }
}