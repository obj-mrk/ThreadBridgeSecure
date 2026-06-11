package mrk.application.usecase;

import mrk.application.command.RegisterUserKeyCommand;
import mrk.application.exception.ConflictException;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserKeyRepository;
import mrk.application.port.UserRepository;
import mrk.application.result.RegisterUserKeyResult;
import mrk.domain.model.User;
import mrk.domain.model.UserKey;
import mrk.domain.value.UserKeyStatus;
import mrk.domain.value.UserStatus;
import mrk.utils.KeyFingerprintCalculator;

import java.time.LocalDateTime;

public class RegisterUserKeyUseCase {
    private static final String SUPPORTED_ALGORITHM = "RSA-OAEP-SHA256";

    private final TransactionManager transactionManager;
    private final UserRepository userRepository;
    private final UserKeyRepository userKeyRepository;
    private final KeyFingerprintCalculator keyFingerprintCalculator;

    public RegisterUserKeyUseCase(
            TransactionManager transactionManager,
            UserRepository userRepository,
            UserKeyRepository userKeyRepository,
            KeyFingerprintCalculator keyFingerprintCalculator
    ) {
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
        this.userKeyRepository = userKeyRepository;
        this.keyFingerprintCalculator = keyFingerprintCalculator;
    }

    public RegisterUserKeyResult register(RegisterUserKeyCommand command) {
        validate(command);

        return transactionManager.execute(connection -> {
            User user = userRepository.findById(connection, command.getUserId())
                    .orElseThrow(() -> new NotFoundException("User not found"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new ValidationException("User is not active");
            }

            String fingerprint = keyFingerprintCalculator.calculate(command.getPublicKeyPem());

            if (userKeyRepository.existsByFingerprint(connection, fingerprint)) {
                throw new ConflictException("Public key fingerprint already exists");
            }

            userKeyRepository.revokeActiveKeysByUserId(connection, command.getUserId());

            UserKey userKey = new UserKey();
            userKey.setUserId(command.getUserId());
            userKey.setPublicKeyPem(command.getPublicKeyPem().trim());
            userKey.setEncryptedPrivateKeyPem(command.getPrivateKeyPem().trim());
            userKey.setKeyFingerprint(fingerprint);
            userKey.setAlgorithm(command.getAlgorithm().trim());
            userKey.setStatus(UserKeyStatus.ACTIVE);
            userKey.setCreatedAt(LocalDateTime.now());
            userKey.setActivatedAt(LocalDateTime.now());
            userKey.setRevokedAt(null);

            long keyId = userKeyRepository.save(connection, userKey);

            return new RegisterUserKeyResult(
                    keyId,
                    command.getUserId(),
                    fingerprint,
                    userKey.getAlgorithm(),
                    userKey.getStatus().name()
            );
        });
    }

    private void validate(RegisterUserKeyCommand command) {
        if (command == null) {
            throw new ValidationException("Command must not be null");
        }

        if (command.getUserId() <= 0) {
            throw new ValidationException("User id must be positive");
        }

        if (command.getPublicKeyPem() == null || command.getPublicKeyPem().isBlank()) {
            throw new ValidationException("Public key must not be blank");
        }

        if (!command.getPublicKeyPem().contains("BEGIN PUBLIC KEY")) {
            throw new ValidationException("Public key must be PEM encoded");
        }

        if (command.getPrivateKeyPem() == null || command.getPrivateKeyPem().isBlank()) {
            throw new ValidationException("Private key must not be blank");
        }

        if (!command.getPrivateKeyPem().contains("BEGIN PRIVATE KEY")) {
            throw new ValidationException("Private key must be PEM encoded");
        }

        if (command.getAlgorithm() == null || command.getAlgorithm().isBlank()) {
            throw new ValidationException("Algorithm must not be blank");
        }

        if (!SUPPORTED_ALGORITHM.equals(command.getAlgorithm())) {
            throw new ValidationException("Unsupported key algorithm: " + command.getAlgorithm());
        }
    }
}