package mrk.infrastructure.crypto;

import mrk.application.crypto.EncryptedPayload;
import mrk.application.port.HybridEncryptionService;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

public class JcaHybridEncryptionService implements HybridEncryptionService {
    public static final String ALGORITHM_VERSION = "AES-256-GCM+RSA-OAEP-SHA256";

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    private static final int AES_KEY_SIZE_BITS = 256;
    private static final int GCM_TAG_SIZE_BITS = 128;
    private static final int GCM_NONCE_SIZE_BYTES = 12;

    private final SecureRandom secureRandom;

    public JcaHybridEncryptionService() {
        this(new SecureRandom());
    }

    public JcaHybridEncryptionService(SecureRandom secureRandom) {
        if (secureRandom == null) {
            throw new IllegalArgumentException("SecureRandom must not be null");
        }

        this.secureRandom = secureRandom;
    }

    @Override
    public EncryptedPayload encrypt(String plaintext, PublicKey recipientPublicKey) {
        validateEncryptInput(plaintext, recipientPublicKey);

        try {
            SecretKey contentKey = generateAesKey();
            byte[] nonce = generateNonce();

            byte[] encryptedPayload = encryptPayloadWithAesGcm(
                    plaintext,
                    contentKey,
                    nonce
            );

            byte[] encryptedContentKey = encryptContentKeyWithRsaOaep(
                    contentKey,
                    recipientPublicKey
            );

            return new EncryptedPayload(
                    encodeBase64(encryptedPayload),
                    encodeBase64(encryptedContentKey),
                    encodeBase64(nonce),
                    ALGORITHM_VERSION
            );
        } catch (Exception exception) {
            throw new CryptoException("Could not encrypt payload", exception);
        }
    }

    @Override
    public String decrypt(EncryptedPayload encryptedPayload, PrivateKey recipientPrivateKey) {
        validateDecryptInput(encryptedPayload, recipientPrivateKey);

        try {
            byte[] encryptedPayloadBytes = decodeBase64(encryptedPayload.getEncryptedPayload());
            byte[] encryptedContentKeyBytes = decodeBase64(encryptedPayload.getEncryptedContentKey());
            byte[] nonce = decodeBase64(encryptedPayload.getNonce());

            SecretKey contentKey = decryptContentKeyWithRsaOaep(
                    encryptedContentKeyBytes,
                    recipientPrivateKey
            );

            byte[] plaintextBytes = decryptPayloadWithAesGcm(
                    encryptedPayloadBytes,
                    contentKey,
                    nonce
            );

            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new CryptoException("Could not decrypt payload", exception);
        }
    }

    private SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(AES_KEY_SIZE_BITS, secureRandom);
        return keyGenerator.generateKey();
    }

    private byte[] generateNonce() {
        byte[] nonce = new byte[GCM_NONCE_SIZE_BYTES];
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    private byte[] encryptPayloadWithAesGcm(
            String plaintext,
            SecretKey contentKey,
            byte[] nonce
    ) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_SIZE_BITS, nonce);

        cipher.init(Cipher.ENCRYPT_MODE, contentKey, gcmParameterSpec);

        return cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] decryptPayloadWithAesGcm(
            byte[] encryptedPayload,
            SecretKey contentKey,
            byte[] nonce
    ) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_SIZE_BITS, nonce);

        cipher.init(Cipher.DECRYPT_MODE, contentKey, gcmParameterSpec);

        return cipher.doFinal(encryptedPayload);
    }

    private byte[] encryptContentKeyWithRsaOaep(
            SecretKey contentKey,
            PublicKey recipientPublicKey
    ) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(
                Cipher.ENCRYPT_MODE,
                recipientPublicKey,
                createOaepSha256ParameterSpec()
        );

        return cipher.doFinal(contentKey.getEncoded());
    }

    private SecretKey decryptContentKeyWithRsaOaep(
            byte[] encryptedContentKey,
            PrivateKey recipientPrivateKey
    ) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(
                Cipher.DECRYPT_MODE,
                recipientPrivateKey,
                createOaepSha256ParameterSpec()
        );

        byte[] contentKeyBytes = cipher.doFinal(encryptedContentKey);

        return new SecretKeySpec(contentKeyBytes, AES_ALGORITHM);
    }

    private OAEPParameterSpec createOaepSha256ParameterSpec() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );
    }

    private String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] decodeBase64(String value) {
        return Base64.getDecoder().decode(value);
    }

    private void validateEncryptInput(String plaintext, PublicKey recipientPublicKey) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new CryptoException("Plaintext must not be blank");
        }

        if (recipientPublicKey == null) {
            throw new CryptoException("Recipient public key must not be null");
        }
    }

    private void validateDecryptInput(EncryptedPayload encryptedPayload, PrivateKey recipientPrivateKey) {
        if (encryptedPayload == null) {
            throw new CryptoException("Encrypted payload must not be null");
        }

        if (recipientPrivateKey == null) {
            throw new CryptoException("Recipient private key must not be null");
        }

        if (!ALGORITHM_VERSION.equals(encryptedPayload.getAlgorithm())) {
            throw new CryptoException("Unsupported encryption algorithm: " + encryptedPayload.getAlgorithm());
        }

        if (encryptedPayload.getEncryptedPayload() == null || encryptedPayload.getEncryptedPayload().isBlank()) {
            throw new CryptoException("Encrypted payload must not be blank");
        }

        if (encryptedPayload.getEncryptedContentKey() == null || encryptedPayload.getEncryptedContentKey().isBlank()) {
            throw new CryptoException("Encrypted content key must not be blank");
        }

        if (encryptedPayload.getNonce() == null || encryptedPayload.getNonce().isBlank()) {
            throw new CryptoException("Nonce must not be blank");
        }
    }
}