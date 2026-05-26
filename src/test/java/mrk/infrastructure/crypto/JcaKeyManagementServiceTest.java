package mrk.infrastructure.crypto;

import mrk.application.port.KeyManagementService;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JcaKeyManagementServiceTest {

    @Test
    void shouldParsePublicKeyFromPem() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();

        String publicKeyPem = toPublicKeyPem(keyPair.getPublic());

        KeyManagementService keyManagementService = new JcaKeyManagementService(
                new RsaPemKeyParser()
        );

        PublicKey parsedPublicKey = keyManagementService.parsePublicKey(publicKeyPem);

        assertNotNull(parsedPublicKey);
        assertEquals("RSA", parsedPublicKey.getAlgorithm());
        assertArrayEquals(keyPair.getPublic().getEncoded(), parsedPublicKey.getEncoded());
    }

    @Test
    void shouldParsePrivateKeyFromPem() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();

        String privateKeyPem = toPrivateKeyPem(keyPair.getPrivate());

        KeyManagementService keyManagementService = new JcaKeyManagementService(
                new RsaPemKeyParser()
        );

        PrivateKey parsedPrivateKey = keyManagementService.parsePrivateKey(privateKeyPem);

        assertNotNull(parsedPrivateKey);
        assertEquals("RSA", parsedPrivateKey.getAlgorithm());
        assertArrayEquals(keyPair.getPrivate().getEncoded(), parsedPrivateKey.getEncoded());
    }

    @Test
    void shouldFailOnInvalidPublicKeyPem() {
        KeyManagementService keyManagementService = new JcaKeyManagementService(
                new RsaPemKeyParser()
        );

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
}