package mrk.application.usecase;

import mrk.application.command.AcceptSecureMessageCommand;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.port.AuditEventRepository;
import mrk.application.port.InboundSecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserRepository;
import mrk.application.result.AcceptSecureMessageResult;
import mrk.domain.model.AuditEvent;
import mrk.domain.model.InboundSecureMessage;
import mrk.domain.model.User;
import mrk.domain.value.AuditEventType;
import mrk.domain.value.InboundMessageStatus;
import mrk.domain.value.UserStatus;

public class AcceptSecureMessageUseCase {
    private final TransactionManager transactionManager;
    private final UserRepository userRepository;
    private final InboundSecureMessageRepository inboundSecureMessageRepository;
    private final AuditEventRepository auditEventRepository;
    private final int maxMessageLength;
    private final int minTtlSeconds;
    private final int maxTtlSeconds;

    public AcceptSecureMessageUseCase(
            TransactionManager transactionManager,
            UserRepository userRepository,
            InboundSecureMessageRepository inboundSecureMessageRepository, AuditEventRepository auditEventRepository,
            int maxMessageLength,
            int minTtlSeconds,
            int maxTtlSeconds
    ) {
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
        this.inboundSecureMessageRepository = inboundSecureMessageRepository;
        this.auditEventRepository = auditEventRepository;
        this.maxMessageLength = maxMessageLength;
        this.minTtlSeconds = minTtlSeconds;
        this.maxTtlSeconds = maxTtlSeconds;
    }

    public AcceptSecureMessageResult accept(AcceptSecureMessageCommand command) {
        validateCommand(command);

        return transactionManager.execute(connection -> {
            return inboundSecureMessageRepository
                    .findByRequestId(connection, command.getRequestId())
                    .map(existing -> new AcceptSecureMessageResult(
                            existing.getId(),
                            existing.getRequestId(),
                            true
                    ))
                    .orElseGet(() -> {
                        User sender = userRepository.findById(connection, command.getSenderId())
                                .orElseThrow(() -> new NotFoundException("Sender not found"));

                        User recipient = userRepository.findById(connection, command.getRecipientId())
                                .orElseThrow(() -> new NotFoundException("Recipient not found"));

                        validateUserIsActive(sender, "Sender is not active");
                        validateUserIsActive(recipient, "Recipient is not active");

                        InboundSecureMessage message = new InboundSecureMessage();
                        message.setRequestId(command.getRequestId().trim());
                        message.setSenderId(command.getSenderId());
                        message.setRecipientId(command.getRecipientId());
                        message.setPlaintextPayload(command.getText());
                        message.setTtlSeconds(command.getTtlSeconds());
                        message.setOneTime(command.isOneTime());
                        message.setStatus(InboundMessageStatus.RECEIVED);

                        long inboundMessageId = inboundSecureMessageRepository.save(connection, message);

                        auditEventRepository.save(
                                connection,
                                AuditEvent.of(
                                        command.getSenderId(),
                                        AuditEventType.INBOUND_ACCEPTED,
                                        "InboundSecureMessage",
                                        inboundMessageId,
                                        "Recipient=" + command.getRecipientId()
                                )
                        );

                        return new AcceptSecureMessageResult(
                                inboundMessageId,
                                command.getRequestId(),
                                false
                        );
                    });
        });
    }

    private void validateCommand(AcceptSecureMessageCommand command) {
        if (command == null) {
            throw new ValidationException("Command must not be null");
        }

        if (isBlank(command.getRequestId())) {
            throw new ValidationException("Request id must not be blank");
        }

        if (command.getRequestId().length() > 150) {
            throw new ValidationException("Request id is too long");
        }

        if (command.getSenderId() <= 0) {
            throw new ValidationException("Sender id must be positive");
        }

        if (command.getRecipientId() <= 0) {
            throw new ValidationException("Recipient id must be positive");
        }

        if (command.getSenderId() == command.getRecipientId()) {
            throw new ValidationException("Sender and recipient must be different users");
        }

        if (isBlank(command.getText())) {
            throw new ValidationException("Message text must not be blank");
        }

        if (command.getText().length() > maxMessageLength) {
            throw new ValidationException("Message text is too long");
        }

        if (command.getTtlSeconds() < minTtlSeconds || command.getTtlSeconds() > maxTtlSeconds) {
            throw new ValidationException("TTL is out of allowed range");
        }
    }

    private void validateUserIsActive(User user, String message) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException(message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}