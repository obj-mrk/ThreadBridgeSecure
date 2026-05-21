package mrk.domain.model;

import mrk.domain.value.UserKeyStatus;

import java.time.LocalDateTime;

public class UserKey {
    private Long id;
    private Long userId;
    private String publicKeyPem;
    private String encryptedPrivateKeyPem;
    private String keyFingerprint;
    private String algorithm;
    private UserKeyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    private LocalDateTime revokedAt;

    public boolean isActive() {
        return status == UserKeyStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Long getUserKey() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserKey(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public String getEncryptedPrivateKeyPem() {
        return encryptedPrivateKeyPem;
    }

    public void setEncryptedPrivateKeyPem(String encryptedPrivateKeyPem) {
        this.encryptedPrivateKeyPem = encryptedPrivateKeyPem;
    }

    public String getKeyFingerprint() {
        return keyFingerprint;
    }

    public void setKeyFingerprint(String keyFingerprint) {
        this.keyFingerprint = keyFingerprint;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public UserKeyStatus getStatus() {
        return status;
    }

    public void setStatus(UserKeyStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}