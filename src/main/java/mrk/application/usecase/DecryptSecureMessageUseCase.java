package mrk.application.usecase;

import mrk.application.command.DecryptSecureMessageCommand;
import mrk.application.crypto.EncryptedPayload;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.port.HybridEncryptionService;
import mrk.application.port.KeyManagementService;
import mrk.application.port.SecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserKeyRepository;
import mrk.application.result.DecryptedMessageResult;
import mrk.domain.model.SecureMessage;
import mrk.domain.model.UserKey;
import mrk.domain.value.SecureMessageStatus;

import java.security.PrivateKey;
import java.time.LocalDateTime;

public class DecryptSecureMessageUseCase {
    private final TransactionManager transactionManager;
    private final SecureMessageRepository secureMessageRepository;
    private final UserKeyRepository userKeyRepository;
    private final KeyManagementService keyManagementService;
    private final HybridEncryptionService hybridEncryptionService;

    public DecryptSecureMessageUseCase(
            TransactionManager transactionManager,
            SecureMessageRepository secureMessageRepository,
            UserKeyRepository userKeyRepository,
            KeyManagementService keyManagementService,
            HybridEncryptionService hybridEncryptionService
    ) {
        this.transactionManager = transactionManager;
        this.secureMessageRepository = secureMessageRepository;
        this.userKeyRepository = userKeyRepository;
        this.keyManagementService = keyManagementService;
        this.hybridEncryptionService = hybridEncryptionService;
    }

    public DecryptedMessageResult decrypt(DecryptSecureMessageCommand command) {
        if (command == null) {
            throw new ValidationException("Command must not be null");
        }

        return transactionManager.execute(connection -> {
            SecureMessage message = secureMessageRepository
                    .findByIdForRecipientForUpdate(
                            connection,
                            command.getMessageId(),
                            command.getRecipientId()
                    )
                    .orElseThrow(() -> new NotFoundException("Secure message not found"));

            validateReadable(message);

            UserKey recipientKey = userKeyRepository
                    .findActiveByUserId(connection, command.getRecipientId())
                    .orElseThrow(() -> new NotFoundException("Recipient active key not found"));

            if (recipientKey.getEncryptedPrivateKeyPem() == null
                    || recipientKey.getEncryptedPrivateKeyPem().isBlank()) {
                throw new ValidationException("Recipient private key is not available");
            }

            PrivateKey privateKey = keyManagementService.parsePrivateKey(
                    recipientKey.getEncryptedPrivateKeyPem()
            );

            EncryptedPayload encryptedPayload = new EncryptedPayload(
                    message.getEncryptedPayload(),
                    message.getEncryptedContentKey(),
                    message.getNonce(),
                    message.getAlgorithm()
            );

            String plaintext = hybridEncryptionService.decrypt(encryptedPayload, privateKey);

            boolean destroyedAfterRead = message.isOneTime();

            if (destroyedAfterRead) {
                secureMessageRepository.markDestroyed(connection, message.getId());
            } else {
                secureMessageRepository.markRead(connection, message.getId());
            }

            return new DecryptedMessageResult(
                    message.getId(),
                    message.getSenderId(),
                    plaintext,
                    destroyedAfterRead
            );
        });
    }

    private void validateReadable(SecureMessage message) {
        LocalDateTime now = LocalDateTime.now();

        if (message.getStatus() == SecureMessageStatus.DESTROYED) {
            throw new ValidationException("Message has already been destroyed");
        }

        if (message.getStatus() == SecureMessageStatus.EXPIRED) {
            throw new ValidationException("Message has expired");
        }

        if (message.isExpired(now)) {
            throw new ValidationException("Message has expired");
        }

        if (!message.isReadable()) {
            throw new ValidationException("Message is not readable in status " + message.getStatus());
        }
    }
}