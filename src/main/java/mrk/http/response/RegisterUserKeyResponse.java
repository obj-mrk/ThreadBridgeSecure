package mrk.http.response;

public class RegisterUserKeyResponse {
    private final String status;
    private final long keyId;
    private final long userId;
    private final String keyFingerprint;
    private final String algorithm;

    public RegisterUserKeyResponse(
            String status,
            long keyId,
            long userId,
            String keyFingerprint,
            String algorithm
    ) {
        this.status = status;
        this.keyId = keyId;
        this.userId = userId;
        this.keyFingerprint = keyFingerprint;
        this.algorithm = algorithm;
    }

    public String getStatus() {
        return status;
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
}