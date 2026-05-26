package mrk.infrastructure.crypto;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaPemKeyParser {
    private static final String RSA_ALGORITHM = "RSA";

    private static final String PUBLIC_KEY_BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_END = "-----END PUBLIC KEY-----";

    private static final String PRIVATE_KEY_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_END = "-----END PRIVATE KEY-----";

    public PublicKey parsePublicKey(String publicKeyPem) {
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new CryptoException("Public key PEM must not be blank");
        }

        try {
            String normalizedPem = normalizePem(publicKeyPem, PUBLIC_KEY_BEGIN, PUBLIC_KEY_END);
            byte[] derBytes = Base64.getDecoder().decode(normalizedPem);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);

            return keyFactory.generatePublic(keySpec);
        } catch (Exception exception) {
            throw new CryptoException("Could not parse RSA public key", exception);
        }
    }

    public PrivateKey parsePrivateKey(String privateKeyPem) {
        if (privateKeyPem == null || privateKeyPem.isBlank()) {
            throw new CryptoException("Private key PEM must not be blank");
        }

        try {
            String normalizedPem = normalizePem(privateKeyPem, PRIVATE_KEY_BEGIN, PRIVATE_KEY_END);
            byte[] derBytes = Base64.getDecoder().decode(normalizedPem);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(derBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);

            return keyFactory.generatePrivate(keySpec);
        } catch (Exception exception) {
            throw new CryptoException("Could not parse RSA private key", exception);
        }
    }

    private String normalizePem(String pem, String beginMarker, String endMarker) {
        String trimmedPem = pem.trim();

        if (!trimmedPem.contains(beginMarker) || !trimmedPem.contains(endMarker)) {
            throw new CryptoException("Invalid PEM format");
        }

        return trimmedPem
                .replace(beginMarker, "")
                .replace(endMarker, "")
                .replaceAll("\\s", "");
    }
}