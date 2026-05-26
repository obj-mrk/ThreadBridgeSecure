package mrk.domain.model;

import mrk.domain.value.InboundMessageStatus;

import java.time.LocalDateTime;

public class InboundSecureMessage {
    private Long id;
    private String requestId;
    private long senderId;
    private long recipientId;
    private String plaintextPayload;
    private int ttlSeconds;
    private boolean oneTime;
    private InboundMessageStatus status;
    private String failureReason;
    private LocalDateTime receivedAt;
    private LocalDateTime processingStartedAt;
    private LocalDateTime processedAt;

    public InboundSecureMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(long recipientId) {
        this.recipientId = recipientId;
    }

    public String getPlaintextPayload() {
        return plaintextPayload;
    }

    public void setPlaintextPayload(String plaintextPayload) {
        this.plaintextPayload = plaintextPayload;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public void setOneTime(boolean oneTime) {
        this.oneTime = oneTime;
    }

    public InboundMessageStatus getStatus() {
        return status;
    }

    public void setStatus(InboundMessageStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
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
}