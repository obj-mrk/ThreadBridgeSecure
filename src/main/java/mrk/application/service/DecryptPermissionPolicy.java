package mrk.application.service;

import mrk.application.exception.ValidationException;
import mrk.domain.model.SecureMessage;
import mrk.domain.value.SecureMessageStatus;

import java.time.LocalDateTime;

public class DecryptPermissionPolicy {

    public void checkCanDecrypt(SecureMessage message, LocalDateTime now) {
        if (message == null) {
            throw new ValidationException("Secure message must not be null");
        }

        if (message.getStatus() == SecureMessageStatus.DESTROYED) {
            throw new ValidationException("Message has already been destroyed");
        }

        if (message.getStatus() == SecureMessageStatus.EXPIRED || message.isExpired(now)) {
            throw new ValidationException("Message has expired");
        }

        if (!message.isReadable()) {
            throw new ValidationException("Message is not readable in status " + message.getStatus());
        }
    }
}