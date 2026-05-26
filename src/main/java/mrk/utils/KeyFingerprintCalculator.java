package mrk.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class KeyFingerprintCalculator {
    public String calculate(String publicKeyPem) {
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new IllegalArgumentException("Public key must not be blank");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalize(publicKeyPem).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalize(String value) {
        return value.trim().replace("\r\n", "\n");
    }
}