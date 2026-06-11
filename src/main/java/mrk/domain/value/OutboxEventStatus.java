package mrk.domain.value;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    PROCESSED,
    FAILED
}
