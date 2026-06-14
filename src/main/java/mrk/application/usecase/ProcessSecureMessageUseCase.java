package mrk.application.usecase;

import mrk.application.command.ProcessSecureMessageCommand;
import mrk.application.crypto.EncryptedPayload;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.port.*;
import mrk.application.result.ProcessSecureMessageResult;
import mrk.domain.model.*;
import mrk.domain.value.AuditEventType;
import mrk.domain.value.InboundMessageStatus;

import java.security.PublicKey;
import java.time.LocalDateTime;

public class ProcessSecureMessageUseCase {
    private final TransactionManager transactionManager;
    private final InboundSecureMessageRepository inboundRepository;
    private final SecureMessageRepository secureMessageRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserRepository userRepository;
    private final UserKeyRepository userKeyRepository;
    private final KeyManagementService keyManagementService;
    private final HybridEncryptionService hybridEncryptionService;
    private final AuditEventRepository auditEventRepository;

    public ProcessSecureMessageUseCase(
            TransactionManager transactionManager,
            InboundSecureMessageRepository inboundRepository,
            SecureMessageRepository secureMessageRepository,
            OutboxEventRepository outboxEventRepository,
            UserRepository userRepository,
            UserKeyRepository userKeyRepository,
            KeyManagementService keyManagementService,
            HybridEncryptionService hybridEncryptionService, AuditEventRepository auditEventRepository
    ) {
        this.transactionManager = transactionManager;
        this.inboundRepository = inboundRepository;
        this.secureMessageRepository = secureMessageRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.userRepository = userRepository;
        this.userKeyRepository = userKeyRepository;
        this.keyManagementService = keyManagementService;
        this.hybridEncryptionService = hybridEncryptionService;
        this.auditEventRepository = auditEventRepository;
    }

    public ProcessSecureMessageResult process(ProcessSecureMessageCommand command) {
        validateCommand(command);

        try {
            return transactionManager.execute(connection -> {
                InboundSecureMessage inbound = inboundRepository
                        .findByIdForUpdate(connection, command.getInboundMessageId())
                        .orElseThrow(() -> new NotFoundException("Inbound secure message not found"));

                validateInboundCanBeProcessed(inbound);

                if (secureMessageRepository.existsByInboundMessageId(connection, inbound.getId())) {
                    inboundRepository.markProcessed(connection, inbound.getId());
                    return new ProcessSecureMessageResult(inbound.getId(), -1);
                }

                User sender = userRepository.findById(connection, inbound.getSenderId())
                        .orElseThrow(() -> new NotFoundException("Sender not found"));

                User recipient = userRepository.findById(connection, inbound.getRecipientId())
                        .orElseThrow(() -> new NotFoundException("Recipient not found"));

                validateUserIsActive(sender, "Sender is not active");
                validateUserIsActive(recipient, "Recipient is not active");

                UserKey recipientKey = userKeyRepository.findActiveByUserId(connection, recipient.getId())
                        .orElseThrow(() -> new NotFoundException("Recipient active public key not found"));

                PublicKey recipientPublicKey =
                        keyManagementService.parsePublicKey(recipientKey.getPublicKeyPem());

                EncryptedPayload encryptedPayload = hybridEncryptionService.encrypt(
                        inbound.getPlaintextPayload(),
                        recipientPublicKey
                );

                LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(inbound.getTtlSeconds());

                SecureMessage secureMessage = SecureMessage.encrypted(
                        inbound.getId(),
                        inbound.getSenderId(),
                        inbound.getRecipientId(),
                        recipientKey.getId(),
                        encryptedPayload.getEncryptedPayload(),
                        encryptedPayload.getEncryptedContentKey(),
                        encryptedPayload.getNonce(),
                        encryptedPayload.getAlgorithm(),
                        inbound.isOneTime(),
                        expiresAt
                );

                long secureMessageId = secureMessageRepository.save(connection, secureMessage);

                auditEventRepository.save(
                        connection,
                        AuditEvent.of(
                                inbound.getSenderId(),
                                AuditEventType.MESSAGE_ENCRYPTED,
                                "SecureMessage",
                                secureMessageId,
                                "Recipient=" + inbound.getRecipientId()
                        )
                );

                outboxEventRepository.save(
                        connection,
                        OutboxEvent.secureMessageEncrypted(secureMessageId, inbound.getRecipientId())
                );

                inboundRepository.markProcessed(connection, inbound.getId());

                return new ProcessSecureMessageResult(inbound.getId(), secureMessageId);
            });
        } catch (RuntimeException exception) {
            markFailedSafely(command.getInboundMessageId(), exception);
            throw exception;
        }
    }

    private void validateCommand(ProcessSecureMessageCommand command) {
        if (command == null) {
            throw new ValidationException("Command must not be null");
        }
    }

    private void validateInboundCanBeProcessed(InboundSecureMessage inbound) {
        if (inbound.getStatus() != InboundMessageStatus.PROCESSING) {
            throw new ValidationException(
                    "Inbound secure message must be in PROCESSING status before encryption"
            );
        }

        if (inbound.getPlaintextPayload() == null || inbound.getPlaintextPayload().isBlank()) {
            throw new ValidationException("Inbound plaintext payload must not be blank");
        }
    }

    private void validateUserIsActive(User user, String message) {
        if (!user.isActive()) {
            throw new ValidationException(message);
        }
    }

    private void markFailedSafely(long inboundMessageId, RuntimeException exception) {
        try {
            transactionManager.execute(connection -> {
                inboundRepository.markFailed(
                        connection,
                        inboundMessageId,
                        sanitizeFailureReason(exception)
                );
                return null;
            });
        } catch (RuntimeException ignored) {
        }
    }

    private String sanitizeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}