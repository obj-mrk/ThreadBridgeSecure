package mrk.domain.model;

import mrk.domain.value.UserKeyStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserKey {

    private final Long id;
    private final Long userId;
    private final String publicKeyPem;
    private final String encryptedPrivateKeyPem;
    private final String keyFingerprint;
    private final String algorithm;
    private final UserKeyStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime activatedAt;
    private final LocalDateTime revokedAt;

    private UserKey(
            Long id,
            Long userId,
            String publicKeyPem,
            String encryptedPrivateKeyPem,
            String keyFingerprint,
            String algorithm,
            UserKeyStatus status,
            LocalDateTime createdAt,
            LocalDateTime activatedAt,
            LocalDateTime revokedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.publicKeyPem = publicKeyPem;
        this.encryptedPrivateKeyPem = encryptedPrivateKeyPem;
        this.keyFingerprint = keyFingerprint;
        this.algorithm = algorithm;
        this.status = status;
        this.createdAt = createdAt;
        this.activatedAt = activatedAt;
        this.revokedAt = revokedAt;
    }

    public static UserKey register(
            Long userId,
            String publicKeyPem,
            String encryptedPrivateKeyPem,
            String keyFingerprint,
            String algorithm,
            LocalDateTime now
    ) {
        validateUserId(userId);
        validatePublicKey(publicKeyPem);
        validateFingerprint(keyFingerprint);
        validateAlgorithm(algorithm);
        requireNonNull(now, "Registration time must not be null");

        return new UserKey(
                null,
                userId,
                publicKeyPem.trim(),
                normalizeNullable(encryptedPrivateKeyPem),
                keyFingerprint.trim(),
                algorithm.trim(),
                UserKeyStatus.ACTIVE,
                now,
                now,
                null
        );
    }

    public static UserKey restore(
            Long id,
            Long userId,
            String publicKeyPem,
            String encryptedPrivateKeyPem,
            String keyFingerprint,
            String algorithm,
            UserKeyStatus status,
            LocalDateTime createdAt,
            LocalDateTime activatedAt,
            LocalDateTime revokedAt
    ) {
        validateId(id);
        validateUserId(userId);
        validatePublicKey(publicKeyPem);
        validateFingerprint(keyFingerprint);
        validateAlgorithm(algorithm);
        requireNonNull(status, "Status must not be null");
        requireNonNull(createdAt, "Created at must not be null");

        validateState(status, activatedAt, revokedAt);

        return new UserKey(
                id,
                userId,
                publicKeyPem.trim(),
                normalizeNullable(encryptedPrivateKeyPem),
                keyFingerprint.trim(),
                algorithm.trim(),
                status,
                createdAt,
                activatedAt,
                revokedAt
        );
    }

    public UserKey revoke(LocalDateTime revokedAt) {
        requireNonNull(revokedAt, "Revoked at must not be null");

        if (status == UserKeyStatus.REVOKED) {
            return this;
        }

        if (status != UserKeyStatus.ACTIVE) {
            throw new IllegalStateException("Only active key can be revoked");
        }

        return new UserKey(
                id,
                userId,
                publicKeyPem,
                encryptedPrivateKeyPem,
                keyFingerprint,
                algorithm,
                UserKeyStatus.REVOKED,
                createdAt,
                activatedAt,
                revokedAt
        );
    }

    public boolean isActive() {
        return status == UserKeyStatus.ACTIVE;
    }

    private static void validateState(
            UserKeyStatus status,
            LocalDateTime activatedAt,
            LocalDateTime revokedAt
    ) {
        if (status == UserKeyStatus.ACTIVE) {
            requireNonNull(activatedAt, "Active key must have activatedAt");

            if (revokedAt != null) {
                throw new IllegalArgumentException("Active key must not have revokedAt");
            }
        }

        if (status == UserKeyStatus.REVOKED) {
            requireNonNull(activatedAt, "Revoked key must have activatedAt");
            requireNonNull(revokedAt, "Revoked key must have revokedAt");
        }
    }

    private static void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User key id must be positive");
        }
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
    }

    private static void validatePublicKey(String publicKeyPem) {
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new IllegalArgumentException("Public key must not be blank");
        }
    }

    private static void validateFingerprint(String keyFingerprint) {
        if (keyFingerprint == null || keyFingerprint.isBlank()) {
            throw new IllegalArgumentException("Key fingerprint must not be blank");
        }
    }

    private static void validateAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("Algorithm must not be blank");
        }
    }

    private static void requireNonNull(Object value, String message) {
        Objects.requireNonNull(value, message);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public String getEncryptedPrivateKeyPem() {
        return encryptedPrivateKeyPem;
    }

    public String getKeyFingerprint() {
        return keyFingerprint;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public UserKeyStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
}