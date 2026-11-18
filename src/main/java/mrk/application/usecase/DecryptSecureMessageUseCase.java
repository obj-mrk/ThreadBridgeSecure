package mrk.application.usecase;

import mrk.application.command.DecryptSecureMessageCommand;
import mrk.application.crypto.EncryptedPayload;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.port.AuditEventRepository;
import mrk.application.port.HybridEncryptionService;
import mrk.application.port.KeyManagementService;
import mrk.application.port.OutboxEventRepository;
import mrk.application.port.SecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserKeyRepository;
import mrk.application.result.DecryptedMessageResult;
import mrk.application.service.DecryptPermissionPolicy;
import mrk.application.service.SecureMessageAuditFactory;
import mrk.application.service.SecureMessageOutboxFactory;
import mrk.domain.model.SecureMessage;
import mrk.domain.model.UserKey;

import java.security.PrivateKey;
import java.time.LocalDateTime;

public class DecryptSecureMessageUseCase {

    private final TransactionManager transactionManager;
    private final SecureMessageRepository secureMessageRepository;
    private final UserKeyRepository userKeyRepository;
    private final KeyManagementService keyManagementService;
    private final HybridEncryptionService hybridEncryptionService;
    private final AuditEventRepository auditEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final DecryptPermissionPolicy decryptPermissionPolicy;
    private final SecureMessageAuditFactory auditFactory;
    private final SecureMessageOutboxFactory outboxFactory;

    public DecryptSecureMessageUseCase(
            TransactionManager transactionManager,
            SecureMessageRepository secureMessageRepository,
            UserKeyRepository userKeyRepository,
            KeyManagementService keyManagementService,
            HybridEncryptionService hybridEncryptionService,
            AuditEventRepository auditEventRepository,
            OutboxEventRepository outboxEventRepository,
            DecryptPermissionPolicy decryptPermissionPolicy,
            SecureMessageAuditFactory auditFactory,
            SecureMessageOutboxFactory outboxFactory
    ) {
        this.transactionManager = transactionManager;
        this.secureMessageRepository = secureMessageRepository;
        this.userKeyRepository = userKeyRepository;
        this.keyManagementService = keyManagementService;
        this.hybridEncryptionService = hybridEncryptionService;
        this.auditEventRepository = auditEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.decryptPermissionPolicy = decryptPermissionPolicy;
        this.auditFactory = auditFactory;
        this.outboxFactory = outboxFactory;
    }

    public DecryptedMessageResult decrypt(DecryptSecureMessageCommand command) {
        validateCommand(command);

        try {
            SecureMessage message = loadMessage(command);
            decryptPermissionPolicy.checkCanDecrypt(message, LocalDateTime.now());

            UserKey recipientKey = loadRecipientKey(message, command.getRecipientId());
            String plaintext = decryptMessage(message, recipientKey);

            finishDecrypt(command);

            return new DecryptedMessageResult(
                    message.getId(),
                    message.getSenderId(),
                    plaintext,
                    message.isOneTime()
            );
        } catch (NotFoundException | ValidationException exception) {
            auditDecryptDeniedSafely(command, exception.getMessage());
            throw exception;
        }
    }

    private SecureMessage loadMessage(DecryptSecureMessageCommand command) {
        return transactionManager.execute(connection ->
                secureMessageRepository
                        .findByIdForRecipient(connection, command.getMessageId(), command.getRecipientId())
                        .orElseThrow(() -> new NotFoundException("Secure message not found"))
        );
    }

    private UserKey loadRecipientKey(SecureMessage message, long recipientId) {
        return transactionManager.execute(connection ->
                userKeyRepository
                        .findByIdForUser(connection, message.getRecipientKeyId(), recipientId)
                        .orElseThrow(() -> new ValidationException("Recipient key not found"))
        );
    }

    private String decryptMessage(SecureMessage message, UserKey recipientKey) {
        // Криптографию выполняем без открытой транзакции.
        PrivateKey privateKey = keyManagementService.parsePrivateKey(
                recipientKey.getEncryptedPrivateKeyPem()
        );

        return hybridEncryptionService.decrypt(
                toEncryptedPayload(message),
                privateKey
        );
    }

    private void finishDecrypt(DecryptSecureMessageCommand command) {
        transactionManager.execute(connection -> {
            // Повторно проверяем состояние под блокировкой.
            SecureMessage message = secureMessageRepository
                    .findByIdForRecipientForUpdate(connection, command.getMessageId(), command.getRecipientId())
                    .orElseThrow(() -> new NotFoundException("Secure message not found"));

            decryptPermissionPolicy.checkCanDecrypt(message, LocalDateTime.now());

            if (message.isOneTime()) {
                secureMessageRepository.markDestroyed(connection, message.getId());
            } else {
                secureMessageRepository.markRead(connection, message.getId());
            }

            auditEventRepository.save(
                    connection,
                    auditFactory.messageRead(command.getRecipientId(), message)
            );

            outboxEventRepository.save(
                    connection,
                    outboxFactory.messageRead(message)
            );

            return null;
        });
    }

    private void auditDecryptDeniedSafely(DecryptSecureMessageCommand command, String reason) {
        try {
            transactionManager.execute(connection -> {
                auditEventRepository.save(
                        connection,
                        auditFactory.decryptDenied(
                                command.getRecipientId(),
                                command.getMessageId(),
                                reason
                        )
                );

                return null;
            });
        } catch (RuntimeException ignored) {
            // Ошибка audit не должна скрывать исходную причину отказа.
        }
    }

    private void validateCommand(DecryptSecureMessageCommand command) {
        if (command == null) {
            throw new ValidationException("Command must not be null");
        }

        if (command.getMessageId() <= 0) {
            throw new ValidationException("Message id must be positive");
        }

        if (command.getRecipientId() <= 0) {
            throw new ValidationException("Recipient id must be positive");
        }
    }

    private EncryptedPayload toEncryptedPayload(SecureMessage message) {
        return new EncryptedPayload(
                message.getEncryptedPayload(),
                message.getEncryptedContentKey(),
                message.getNonce(),
                message.getAlgorithm()
        );
    }
}