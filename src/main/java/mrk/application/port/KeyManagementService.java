package mrk.application.port;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface KeyManagementService {

    PublicKey parsePublicKey(String publicKeyPem);

    PrivateKey parsePrivateKey(String privateKeyPem);

    void validateKeyPair(String publicKeyPem, String privateKeyPem);

    String encryptPrivateKey(String privateKeyPem);

    String decryptPrivateKey(String encryptedPrivateKeyPem);
}