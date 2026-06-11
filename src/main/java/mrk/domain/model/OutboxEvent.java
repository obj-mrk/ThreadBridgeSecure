package mrk.domain.model;

import mrk.domain.value.OutboxEventStatus;

import java.time.LocalDateTime;

public class OutboxEvent {
    private Long id;
    private String eventType;
    private String aggregateType;
    private long aggregateId;
    private String payload;
    private OutboxEventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processingStartedAt;
    private LocalDateTime processedAt;
    private String failureReason;

    public static OutboxEvent secureMessageEncrypted(long secureMessageId, long recipientId) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("SECURE_MESSAGE_ENCRYPTED");
        event.setAggregateType("SecureMessage");
        event.setAggregateId(secureMessageId);

        event.setPayload("""
                {"secureMessageId":%d,"recipientId":%d}
                """.formatted(secureMessageId, recipientId).trim());

        event.setStatus(OutboxEventStatus.NEW);
        return event;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public long getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxEventStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}