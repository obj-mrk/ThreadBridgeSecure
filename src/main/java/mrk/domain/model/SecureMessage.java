package mrk.domain.model;

import mrk.domain.value.SecureMessageStatus;

import java.time.LocalDateTime;

public class SecureMessage {
    private Long id;
    private long inboundMessageId;
    private long senderId;
    private long recipientId;
    private long recipientKeyId;
    private String encryptedPayload;
    private String encryptedContentKey;
    private String nonce;
    private String algorithm;
    private SecureMessageStatus status;
    private boolean oneTime;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime readAt;
    private LocalDateTime destroyedAt;

    public static SecureMessage encrypted(
            long inboundMessageId,
            long senderId,
            long recipientId,
            long recipientKeyId,
            String encryptedPayload,
            String encryptedContentKey,
            String nonce,
            String algorithm,
            boolean oneTime,
            LocalDateTime expiresAt
    ) {
        SecureMessage message = new SecureMessage();
        message.setInboundMessageId(inboundMessageId);
        message.setSenderId(senderId);
        message.setRecipientId(recipientId);
        message.setRecipientKeyId(recipientKeyId);
        message.setEncryptedPayload(encryptedPayload);
        message.setEncryptedContentKey(encryptedContentKey);
        message.setNonce(nonce);
        message.setAlgorithm(algorithm);
        message.setOneTime(oneTime);
        message.setExpiresAt(expiresAt);

        message.setStatus(SecureMessageStatus.ENCRYPTED);

        return message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getInboundMessageId() {
        return inboundMessageId;
    }

    public void setInboundMessageId(long inboundMessageId) {
        this.inboundMessageId = inboundMessageId;
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

    public long getRecipientKeyId() {
        return recipientKeyId;
    }

    public void setRecipientKeyId(long recipientKeyId) {
        this.recipientKeyId = recipientKeyId;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(String encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    public String getEncryptedContentKey() {
        return encryptedContentKey;
    }

    public void setEncryptedContentKey(String encryptedContentKey) {
        this.encryptedContentKey = encryptedContentKey;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public SecureMessageStatus getStatus() {
        return status;
    }

    public void setStatus(SecureMessageStatus status) {
        this.status = status;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public void setOneTime(boolean oneTime) {
        this.oneTime = oneTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getDestroyedAt() {
        return destroyedAt;
    }

    public void setDestroyedAt(LocalDateTime destroyedAt) {
        this.destroyedAt = destroyedAt;
    }
}