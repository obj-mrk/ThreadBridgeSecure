package mrk.infrastructure.crypto;

import mrk.application.port.KeyManagementService;

import java.security.PrivateKey;
import java.security.PublicKey;

public class JcaKeyManagementService implements KeyManagementService {
    private final RsaPemKeyParser rsaPemKeyParser;

    public JcaKeyManagementService(RsaPemKeyParser rsaPemKeyParser) {
        this.rsaPemKeyParser = rsaPemKeyParser;
    }

    @Override
    public PublicKey parsePublicKey(String publicKeyPem) {
        return rsaPemKeyParser.parsePublicKey(publicKeyPem);
    }

    @Override
    public PrivateKey parsePrivateKey(String privateKeyPem) {
        return rsaPemKeyParser.parsePrivateKey(privateKeyPem);
    }
}