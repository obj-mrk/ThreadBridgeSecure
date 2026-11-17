package mrk.application.usecase;

import mrk.application.command.ProcessSecureMessageCommand;
import mrk.application.crypto.EncryptedPayload;
import mrk.application.exception.ConflictException;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.port.AuditEventRepository;
import mrk.application.port.HybridEncryptionService;
import mrk.application.port.InboundSecureMessageRepository;
import mrk.application.port.KeyManagementService;
import mrk.application.port.OutboxEventRepository;
import mrk.application.port.SecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserKeyRepository;
import mrk.application.port.UserRepository;
import mrk.application.result.ProcessSecureMessageResult;
import mrk.domain.model.AuditEvent;
import mrk.domain.model.InboundSecureMessage;
import mrk.domain.model.OutboxEvent;
import mrk.domain.model.SecureMessage;
import mrk.domain.model.User;
import mrk.domain.model.UserKey;
import mrk.domain.value.AuditEventType;
import mrk.domain.value.InboundMessageStatus;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Optional;

public class ProcessSecureMessageUseCase {

    private static final int MAX_KEY_ROTATION_ATTEMPTS = 3;

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
            HybridEncryptionService hybridEncryptionService,
            AuditEventRepository auditEventRepository
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
            for (int attempt = 0; attempt < MAX_KEY_ROTATION_ATTEMPTS; attempt++) {

                Optional<Long> existingMessageId =
                        findExistingSecureMessage(command.getInboundMessageId());

                if (existingMessageId.isPresent()) {
                    return new ProcessSecureMessageResult(
                            command.getInboundMessageId(),
                            existingMessageId.get()
                    );
                }

                InboundSecureMessage inbound = loadInbound(command.getInboundMessageId());

                UserKey recipientKey = loadRecipientKey(inbound);

                EncryptedPayload encryptedPayload = encrypt(inbound, recipientKey);

                Long secureMessageId =
                        saveIfKeyStillActive(
                                inbound,
                                recipientKey,
                                encryptedPayload
                        );

                if (secureMessageId != null) {
                    return new ProcessSecureMessageResult(
                            inbound.getId(),
                            secureMessageId
                    );
                }
            }

            throw new ConflictException(
                    "Recipient key changed too many times during encryption"
            );

        } catch (RuntimeException exception) {
            markFailedSafely(
                    command.getInboundMessageId(),
                    exception
            );

            throw exception;
        }
    }

    private Optional<Long> findExistingSecureMessage(long inboundMessageId) {
        return transactionManager.execute(connection -> {
            Optional<Long> secureMessageId =
                    secureMessageRepository.findIdByInboundMessageId(
                            connection,
                            inboundMessageId
                    );

            if (secureMessageId.isPresent()) {
                inboundRepository.markProcessedAndClearPlaintext(
                        connection,
                        inboundMessageId
                );
            }

            return secureMessageId;
        });
    }

    private InboundSecureMessage loadInbound(long inboundMessageId) {
        return transactionManager.execute(connection -> {
            InboundSecureMessage inbound = inboundRepository
                    .findByIdForUpdate(connection, inboundMessageId)
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Inbound secure message not found"
                            )
                    );

            validateInboundCanBeProcessed(inbound);

            return inbound;
        });
    }

    private UserKey loadRecipientKey(InboundSecureMessage inbound) {
        return transactionManager.execute(connection -> {
            User sender = userRepository
                    .findById(connection, inbound.getSenderId())
                    .orElseThrow(() ->
                            new NotFoundException("Sender not found")
                    );

            User recipient = userRepository
                    .findById(connection, inbound.getRecipientId())
                    .orElseThrow(() ->
                            new NotFoundException("Recipient not found")
                    );

            validateUserIsActive(sender, "Sender is not active");
            validateUserIsActive(recipient, "Recipient is not active");

            return userKeyRepository
                    .findActiveByUserId(connection, recipient.getId())
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Recipient active public key not found"
                            )
                    );
        });
    }

    private EncryptedPayload encrypt(InboundSecureMessage inbound, UserKey recipientKey) {
        // Шифрование выполняем без открытой транзакции.
        PublicKey publicKey =
                keyManagementService.parsePublicKey(
                        recipientKey.getPublicKeyPem()
                );

        return hybridEncryptionService.encrypt(
                inbound.getPlaintextPayload(),
                publicKey
        );
    }

    private Long saveIfKeyStillActive(
            InboundSecureMessage inbound,
            UserKey recipientKey,
            EncryptedPayload encryptedPayload
    ) {
        return transactionManager.execute(connection -> {

            // Та же блокировка используется при rotation ключа.
            User recipient = userRepository
                    .findByIdForUpdate(
                            connection,
                            inbound.getRecipientId()
                    )
                    .orElseThrow(() ->
                            new NotFoundException("Recipient not found")
                    );

            validateUserIsActive(recipient, "Recipient is not active");

            Optional<Long> existingMessageId =
                    secureMessageRepository.findIdByInboundMessageId(
                            connection,
                            inbound.getId()
                    );

            if (existingMessageId.isPresent()) {
                inboundRepository.markProcessedAndClearPlaintext(
                        connection,
                        inbound.getId()
                );

                return existingMessageId.get();
            }

            UserKey activeKey = userKeyRepository
                    .findActiveByUserId(
                            connection,
                            inbound.getRecipientId()
                    )
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Recipient active public key not found"
                            )
                    );

            // Ключ изменился во время шифрования.
            if (activeKey.getId() != recipientKey.getId()) {
                return null;
            }

            SecureMessage secureMessage = createSecureMessage(inbound, recipientKey, encryptedPayload);

            long secureMessageId = secureMessageRepository.save(connection, secureMessage);

            saveAudit(
                    connection,
                    inbound,
                    secureMessageId
            );

            outboxEventRepository.save(
                    connection,
                    OutboxEvent.secureMessageEncrypted(
                            secureMessageId,
                            inbound.getRecipientId()
                    )
            );

            inboundRepository.markProcessedAndClearPlaintext(
                    connection,
                    inbound.getId()
            );

            return secureMessageId;
        });
    }

    private SecureMessage createSecureMessage(
            InboundSecureMessage inbound,
            UserKey recipientKey,
            EncryptedPayload encryptedPayload
    ) {
        return SecureMessage.encrypted(
                inbound.getId(),
                inbound.getSenderId(),
                inbound.getRecipientId(),
                recipientKey.getId(),
                encryptedPayload.getEncryptedPayload(),
                encryptedPayload.getEncryptedContentKey(),
                encryptedPayload.getNonce(),
                encryptedPayload.getAlgorithm(),
                inbound.isOneTime(),
                LocalDateTime.now().plusSeconds(inbound.getTtlSeconds())
        );
    }

    private void saveAudit(
            java.sql.Connection connection,
            InboundSecureMessage inbound,
            long secureMessageId
    ) {
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
    }

    private void validateCommand(ProcessSecureMessageCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Command must not be null"
            );
        }
    }

    private void validateInboundCanBeProcessed(InboundSecureMessage inbound) {
        if (inbound.getStatus() != InboundMessageStatus.PROCESSING) {
            throw new ValidationException(
                    "Inbound secure message must be in PROCESSING status before encryption"
            );
        }

        if (inbound.getPlaintextPayload() == null || inbound.getPlaintextPayload().isBlank()) {
            throw new ValidationException(
                    "Inbound plaintext payload must not be blank"
            );
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

        return message.length() > 500
                ? message.substring(0, 500)
                : message;
    }
}