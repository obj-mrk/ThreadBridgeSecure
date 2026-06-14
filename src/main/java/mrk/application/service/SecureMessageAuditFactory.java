package mrk.application.service;

import mrk.domain.model.AuditEvent;
import mrk.domain.model.SecureMessage;
import mrk.domain.value.AuditEventType;

public class SecureMessageAuditFactory {

    private static final String AGGREGATE_TYPE = "SecureMessage";

    public AuditEvent messageRead(long actorUserId, SecureMessage message) {
        return AuditEvent.of(
                actorUserId,
                AuditEventType.MESSAGE_READ,
                AGGREGATE_TYPE,
                message.getId(),
                message.isOneTime()
                        ? "Secure message read and destroyed because oneTime=true"
                        : "Secure message read"
        );
    }

    public AuditEvent decryptDenied(long actorUserId, Long messageId, String reason) {
        return AuditEvent.of(
                actorUserId,
                AuditEventType.DECRYPT_DENIED,
                AGGREGATE_TYPE,
                messageId,
                reason
        );
    }

    public AuditEvent messagesExpired(int expiredCount) {
        return AuditEvent.of(
                null,
                AuditEventType.MESSAGE_EXPIRED,
                AGGREGATE_TYPE,
                null,
                "Expired messages batch size=" + expiredCount
        );
    }
}