package mrk.application.command;

public class RegisterUserKeyCommand {
    private long userId;
    private String publicKeyPem;
    private String algorithm;

    public RegisterUserKeyCommand() {
    }

    public RegisterUserKeyCommand(long userId, String publicKeyPem, String algorithm) {
        this.userId = userId;
        this.publicKeyPem = publicKeyPem;
        this.algorithm = algorithm;
    }

    public long getUserId() {
        return userId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}