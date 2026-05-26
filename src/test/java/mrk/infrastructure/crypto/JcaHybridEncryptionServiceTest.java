package mrk.infrastructure.crypto;

import mrk.application.crypto.EncryptedPayload;
import mrk.application.port.HybridEncryptionService;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.*;

class JcaHybridEncryptionServiceTest {

    @Test
    void shouldEncryptAndDecryptPayload() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        HybridEncryptionService encryptionService = new JcaHybridEncryptionService();

        String plaintext = "confidential payload";

        EncryptedPayload encryptedPayload = encryptionService.encrypt(
                plaintext,
                keyPair.getPublic()
        );

        String decryptedPayload = encryptionService.decrypt(
                encryptedPayload,
                keyPair.getPrivate()
        );

        assertEquals(plaintext, decryptedPayload);
        assertNotEquals(plaintext, encryptedPayload.getEncryptedPayload());
        assertNotNull(encryptedPayload.getEncryptedContentKey());
        assertNotNull(encryptedPayload.getNonce());
        assertEquals(
                JcaHybridEncryptionService.ALGORITHM_VERSION,
                encryptedPayload.getAlgorithm()
        );
    }

    @Test
    void shouldProduceDifferentCiphertextForSamePlaintext() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        HybridEncryptionService encryptionService = new JcaHybridEncryptionService();

        String plaintext = "same payload";

        EncryptedPayload first = encryptionService.encrypt(
                plaintext,
                keyPair.getPublic()
        );

        EncryptedPayload second = encryptionService.encrypt(
                plaintext,
                keyPair.getPublic()
        );

        assertNotEquals(first.getEncryptedPayload(), second.getEncryptedPayload());
        assertNotEquals(first.getEncryptedContentKey(), second.getEncryptedContentKey());
        assertNotEquals(first.getNonce(), second.getNonce());
    }

    @Test
    void shouldFailDecryptWhenPrivateKeyDoesNotMatch() throws Exception {
        KeyPair recipientKeyPair = generateRsaKeyPair();
        KeyPair anotherKeyPair = generateRsaKeyPair();

        HybridEncryptionService encryptionService = new JcaHybridEncryptionService();

        EncryptedPayload encryptedPayload = encryptionService.encrypt(
                "secret message",
                recipientKeyPair.getPublic()
        );

        assertThrows(
                CryptoException.class,
                () -> encryptionService.decrypt(encryptedPayload, anotherKeyPair.getPrivate())
        );
    }

    @Test
    void shouldFailDecryptWhenCiphertextWasModified() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        HybridEncryptionService encryptionService = new JcaHybridEncryptionService();

        EncryptedPayload encryptedPayload = encryptionService.encrypt(
                "secret message",
                keyPair.getPublic()
        );

        EncryptedPayload corruptedPayload = new EncryptedPayload(
                encryptedPayload.getEncryptedPayload().substring(1),
                encryptedPayload.getEncryptedContentKey(),
                encryptedPayload.getNonce(),
                encryptedPayload.getAlgorithm()
        );

        assertThrows(
                CryptoException.class,
                () -> encryptionService.decrypt(corruptedPayload, keyPair.getPrivate())
        );
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }
}