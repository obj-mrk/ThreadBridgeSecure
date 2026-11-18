package mrk.infrastructure.crypto;

import mrk.application.port.KeyManagementService;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class JcaKeyManagementService implements KeyManagementService {

    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_LENGTH_BYTES = 32;
    private static final int GCM_NONCE_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String FORMAT_VERSION = "v1";

    private final RsaPemKeyParser rsaPemKeyParser;
    private final SecretKeySpec masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public JcaKeyManagementService(RsaPemKeyParser rsaPemKeyParser, String masterKeyBase64) {
        this.rsaPemKeyParser = rsaPemKeyParser;
        this.masterKey = new SecretKeySpec(decodeMasterKey(masterKeyBase64), "AES");
    }

    @Override
    public PublicKey parsePublicKey(String publicKeyPem) {
        return rsaPemKeyParser.parsePublicKey(publicKeyPem);
    }

    @Override
    public PrivateKey parsePrivateKey(String privateKeyPem) {
        return rsaPemKeyParser.parsePrivateKey(privateKeyPem);
    }

    @Override
    public void validateKeyPair(String publicKeyPem, String privateKeyPem) {
        PublicKey publicKey = parsePublicKey(publicKeyPem);
        PrivateKey privateKey = parsePrivateKey(privateKeyPem);

        if (!(publicKey instanceof RSAPublicKey rsaPublicKey) ||
                !(privateKey instanceof RSAPrivateKey rsaPrivateKey)) {
            throw new CryptoException("Expected RSA key pair");
        }

        if (!rsaPublicKey.getModulus().equals(rsaPrivateKey.getModulus())) {
            throw new CryptoException("RSA public and private keys do not match");
        }
    }

    @Override
    public String encryptPrivateKey(String privateKeyPem) {
        if (privateKeyPem == null || privateKeyPem.isBlank()) {
            throw new CryptoException("Private key must not be blank");
        }

        try {
            byte[] nonce = new byte[GCM_NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    masterKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
            );

            byte[] ciphertext = cipher.doFinal(
                    privateKeyPem.getBytes(StandardCharsets.UTF_8)
            );

            return FORMAT_VERSION
                    + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce)
                    + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);

        } catch (Exception exception) {
            throw new CryptoException("Could not encrypt private key", exception);
        }
    }

    @Override
    public String decryptPrivateKey(String encryptedPrivateKeyPem) {
        if (encryptedPrivateKeyPem == null || encryptedPrivateKeyPem.isBlank()) {
            throw new CryptoException("Encrypted private key must not be blank");
        }

        try {
            String[] parts = encryptedPrivateKeyPem.split(":", 3);

            if (parts.length != 3 || !FORMAT_VERSION.equals(parts[0])) {
                throw new CryptoException("Unsupported encrypted private key format");
            }

            byte[] nonce = Base64.getUrlDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[2]);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    masterKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
            );

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (CryptoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CryptoException("Could not decrypt private key", exception);
        }
    }

    private byte[] decodeMasterKey(String masterKeyBase64) {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            throw new IllegalArgumentException("Master key must be configured");
        }

        try {
            byte[] masterKeyBytes = Base64.getDecoder().decode(masterKeyBase64);

            if (masterKeyBytes.length != AES_KEY_LENGTH_BYTES) {
                throw new IllegalArgumentException("Master key must contain exactly 32 bytes");
            }

            return masterKeyBytes;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid master key", exception);
        }
    }
}