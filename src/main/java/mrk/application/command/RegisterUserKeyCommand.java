package mrk.application.command;

public class RegisterUserKeyCommand {
    private final long userId;
    private final String publicKeyPem;
    private final String privateKeyPem;
    private final String algorithm;

    public RegisterUserKeyCommand(
            long userId,
            String publicKeyPem,
            String privateKeyPem,
            String algorithm
    ) {
        this.userId = userId;
        this.publicKeyPem = publicKeyPem;
        this.privateKeyPem = privateKeyPem;
        this.algorithm = algorithm;
    }

    public long getUserId() {
        return userId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}