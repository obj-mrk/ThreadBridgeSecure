package mrk.infrastructure.crypto;

import mrk.application.port.KeyManagementService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JcaKeyManagementServiceTest {
    private static final String TEST_MASTER_KEY = Base64.getEncoder()
            .encodeToString(
                    "0123456789abcdef0123456789abcdef"
                            .getBytes(StandardCharsets.UTF_8)
            );

    private KeyManagementService createService() {
        return new JcaKeyManagementService(
                new RsaPemKeyParser(),
                TEST_MASTER_KEY
        );
    }

    KeyManagementService keyManagementService;

    @Test
    void shouldParsePublicKeyFromPem() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();

        String publicKeyPem = toPublicKeyPem(keyPair.getPublic());


        PublicKey parsedPublicKey = keyManagementService.parsePublicKey(publicKeyPem);

        assertNotNull(parsedPublicKey);
        assertEquals("RSA", parsedPublicKey.getAlgorithm());
        assertArrayEquals(keyPair.getPublic().getEncoded(), parsedPublicKey.getEncoded());
    }

    @Test
    void shouldParsePrivateKeyFromPem() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();

        String privateKeyPem = toPrivateKeyPem(keyPair.getPrivate());
        PrivateKey parsedPrivateKey = keyManagementService.parsePrivateKey(privateKeyPem);

        assertNotNull(parsedPrivateKey);
        assertEquals("RSA", parsedPrivateKey.getAlgorithm());
        assertArrayEquals(keyPair.getPrivate().getEncoded(), parsedPrivateKey.getEncoded());
    }

    @Test
    void shouldFailOnInvalidPublicKeyPem() {
        String invalidPem = """
                -----BEGIN PUBLIC KEY-----
                INVALID_KEY_DATA
                -----END PUBLIC KEY-----
                """;

        assertThrows(
                CryptoException.class,
                () -> keyManagementService.parsePublicKey(invalidPem)
        );
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    private String toPublicKeyPem(PublicKey publicKey) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(publicKey.getEncoded());

        return """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """.formatted(encoded);
    }

    private String toPrivateKeyPem(PrivateKey privateKey) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(privateKey.getEncoded());

        return """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(encoded);
    }

    @Test
    void shouldEncryptAndDecryptPrivateKey() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String privateKeyPem = toPrivateKeyPem(keyPair.getPrivate());

        KeyManagementService service = createService();

        String encrypted = service.encryptPrivateKey(privateKeyPem);
        String decrypted = service.decryptPrivateKey(encrypted);

        assertNotEquals(privateKeyPem, encrypted);
        assertFalse(encrypted.contains("BEGIN PRIVATE KEY"));
        assertEquals(privateKeyPem, decrypted);
    }

    @Test
    void shouldValidateMatchingKeyPair() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();

        KeyManagementService service = createService();

        assertDoesNotThrow(() ->
                service.validateKeyPair(
                        toPublicKeyPem(keyPair.getPublic()),
                        toPrivateKeyPem(keyPair.getPrivate())
                )
        );
    }

    @Test
    void shouldRejectDifferentKeyPair() throws Exception {
        KeyPair first = generateRsaKeyPair();
        KeyPair second = generateRsaKeyPair();

        KeyManagementService service = createService();

        assertThrows(
                CryptoException.class,
                () -> service.validateKeyPair(
                        toPublicKeyPem(first.getPublic()),
                        toPrivateKeyPem(second.getPrivate())
                )
        );
    }
}