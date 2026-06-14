package mrk.application.service;

import mrk.domain.model.OutboxEvent;
import mrk.domain.model.SecureMessage;

public class SecureMessageOutboxFactory {

    public OutboxEvent messageRead(SecureMessage message) {
        return OutboxEvent.secureMessageRead(
                message.getId(),
                message.getSenderId(),
                message.getRecipientId(),
                message.isOneTime()
        );
    }

    public OutboxEvent messagesExpired(int expiredCount) {
        return OutboxEvent.secureMessagesExpired(expiredCount);
    }
}