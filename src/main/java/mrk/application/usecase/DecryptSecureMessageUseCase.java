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
import java.sql.Connection;
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

        return transactionManager.execute(connection -> {
            SecureMessage message = secureMessageRepository
                    .findByIdForRecipientForUpdate(
                            connection,
                            command.getMessageId(),
                            command.getRecipientId()
                    )
                    .orElseThrow(() -> {
                        auditDecryptDenied(
                                connection,
                                command.getRecipientId(),
                                command.getMessageId(),
                                "Secure message not found or access denied"
                        );
                        return new NotFoundException("Secure message not found");
                    });

            try {
                decryptPermissionPolicy.checkCanDecrypt(message, LocalDateTime.now());
            } catch (ValidationException exception) {
                auditDecryptDenied(
                        connection,
                        command.getRecipientId(),
                        message.getId(),
                        exception.getMessage()
                );
                throw exception;
            }

            UserKey recipientKey = userKeyRepository
                    .findActiveByUserId(connection, command.getRecipientId())
                    .orElseThrow(() -> {
                        auditDecryptDenied(
                                connection,
                                command.getRecipientId(),
                                message.getId(),
                                "Recipient active key not found"
                        );
                        return new ValidationException("Recipient active key not found");
                    });

            PrivateKey privateKey = keyManagementService.parsePrivateKey(
                    recipientKey.getEncryptedPrivateKeyPem()
            );

            String plaintext = hybridEncryptionService.decrypt(
                    toEncryptedPayload(message),
                    privateKey
            );

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

            return new DecryptedMessageResult(
                    message.getId(),
                    message.getSenderId(),
                    plaintext,
                    message.isOneTime()
            );
        });
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

    private void auditDecryptDenied(
            Connection connection,
            long actorUserId,
            long messageId,
            String reason
    ) {
        auditEventRepository.save(
                connection,
                auditFactory.decryptDenied(actorUserId, messageId, reason)
        );
    }
}