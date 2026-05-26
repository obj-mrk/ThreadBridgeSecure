package mrk.application.port;

import mrk.application.crypto.EncryptedPayload;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface HybridEncryptionService {
    EncryptedPayload encrypt(String plaintext, PublicKey recipientPublicKey);

    String decrypt(EncryptedPayload encryptedPayload, PrivateKey recipientPrivateKey);
}
