package mrk.application.result;

public class RegisterUserKeyResult {
    private final long keyId;
    private final long userId;
    private final String keyFingerprint;
    private final String algorithm;
    private final String status;

    public RegisterUserKeyResult(
            long keyId,
            long userId,
            String keyFingerprint,
            String algorithm,
            String status
    ) {
        this.keyId = keyId;
        this.userId = userId;
        this.keyFingerprint = keyFingerprint;
        this.algorithm = algorithm;
        this.status = status;
    }

    public long getKeyId() {
        return keyId;
    }

    public long getUserId() {
        return userId;
    }

    public String getKeyFingerprint() {
        return keyFingerprint;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getStatus() {
        return status;
    }
}