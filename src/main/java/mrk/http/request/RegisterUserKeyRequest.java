package mrk.http.request;

public class RegisterUserKeyRequest {
    private String publicKeyPem;
    private String algorithm;

    public RegisterUserKeyRequest() {
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
