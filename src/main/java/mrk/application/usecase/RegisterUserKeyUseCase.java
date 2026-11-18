package mrk.application.usecase;

import mrk.application.command.RegisterUserKeyCommand;
import mrk.application.exception.ConflictException;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.port.KeyManagementService;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserKeyRepository;
import mrk.application.port.UserRepository;
import mrk.application.result.RegisterUserKeyResult;
import mrk.domain.model.UserKey;
import mrk.utils.KeyFingerprintCalculator;

import java.time.LocalDateTime;

public class RegisterUserKeyUseCase {

    private final TransactionManager transactionManager;
    private final UserRepository userRepository;
    private final UserKeyRepository userKeyRepository;
    private final KeyFingerprintCalculator keyFingerprintCalculator;
    private final KeyManagementService keyManagementService;

    public RegisterUserKeyUseCase(
            TransactionManager transactionManager,
            UserRepository userRepository,
            UserKeyRepository userKeyRepository,
            KeyFingerprintCalculator keyFingerprintCalculator,
            KeyManagementService keyManagementService
    ) {
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
        this.userKeyRepository = userKeyRepository;
        this.keyFingerprintCalculator = keyFingerprintCalculator;
        this.keyManagementService = keyManagementService;
    }

    public RegisterUserKeyResult register(RegisterUserKeyCommand command) {
        validate(command);

        keyManagementService.validateKeyPair(
                command.getPublicKeyPem(),
                command.getPrivateKeyPem()
        );

        String fingerprint = keyFingerprintCalculator.calculate(
                command.getPublicKeyPem()
        );

        // Криптографию выполняем до открытия транзакции.
        String encryptedPrivateKeyPem = keyManagementService.encryptPrivateKey(
                command.getPrivateKeyPem()
        );

        return transactionManager.execute(connection -> {
            userRepository.findByIdForUpdate(connection, command.getUserId())
                    .orElseThrow(() -> new NotFoundException("User not found"));

            if (userKeyRepository.existsByFingerprint(connection, fingerprint)) {
                throw new ConflictException("Public key fingerprint already exists");
            }

            userKeyRepository.revokeActiveKeysByUserId(
                    connection,
                    command.getUserId()
            );

            UserKey userKey = UserKey.register(
                    command.getUserId(),
                    command.getPublicKeyPem(),
                    encryptedPrivateKeyPem,
                    fingerprint,
                    command.getAlgorithm(),
                    LocalDateTime.now()
            );

            long keyId = userKeyRepository.save(
                    connection,
                    userKey
            );

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

        if (command.getPrivateKeyPem() == null || command.getPrivateKeyPem().isBlank()) {
            throw new ValidationException("Private key must not be blank");
        }

        if (command.getAlgorithm() == null || command.getAlgorithm().isBlank()) {
            throw new ValidationException("Algorithm must not be blank");
        }
    }
}