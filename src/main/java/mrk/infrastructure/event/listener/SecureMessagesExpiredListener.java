package mrk.infrastructure.event.listener;

import mrk.application.port.SecureInboxCache;
import mrk.domain.model.OutboxEvent;
import mrk.infrastructure.event.EventListener;
import mrk.infrastructure.event.payload.SecureMessagesExpiredPayload;
import mrk.utils.JsonUtils;

import java.sql.Connection;

public class SecureMessagesExpiredListener implements EventListener {

    private final SecureInboxCache secureInboxCache;
    private final JsonUtils jsonUtils;

    public SecureMessagesExpiredListener(SecureInboxCache secureInboxCache, JsonUtils jsonUtils) {
        this.secureInboxCache = secureInboxCache;
        this.jsonUtils = jsonUtils;
    }

    @Override
    public String eventType() {
        return "SECURE_MESSAGES_EXPIRED";
    }

    @Override
    public void handle(Connection connection, OutboxEvent event) {
        SecureMessagesExpiredPayload payload = jsonUtils.fromJson(
                event.getPayload(),
                SecureMessagesExpiredPayload.class
        );

        for (Long recipientId : payload.getRecipientIds()) {
            secureInboxCache.evict(recipientId);
        }
    }
}