package mrk.application.crypto;

public class EncryptedPayload {
    private final String encryptedPayload;
    private final String encryptedContentKey;
    private final String nonce;
    private final String algorithm;

    public EncryptedPayload(String encryptedPayload,
                            String encryptedContentKey,
                            String nonce,
                            String algorithm) {
        this.encryptedPayload = encryptedPayload;
        this.encryptedContentKey = encryptedContentKey;
        this.nonce = nonce;
        this.algorithm = algorithm;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public String getEncryptedContentKey() {
        return encryptedContentKey;
    }

    public String getNonce() {
        return nonce;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
