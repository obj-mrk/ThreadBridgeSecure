package mrk.infrastructure.event.listener;

import mrk.application.port.NotificationRepository;
import mrk.application.port.SecureInboxCache;
import mrk.domain.model.Notification;
import mrk.domain.model.OutboxEvent;
import mrk.infrastructure.event.EventListener;
import mrk.infrastructure.event.payload.SecureMessageEncryptedPayload;
import mrk.utils.JsonUtils;

import java.sql.Connection;

public class SecureMessageEncryptedListener implements EventListener {

    private final NotificationRepository notificationRepository;
    private final SecureInboxCache secureInboxCache;
    private final JsonUtils jsonUtils;

    public SecureMessageEncryptedListener(
            NotificationRepository notificationRepository,
            SecureInboxCache secureInboxCache,
            JsonUtils jsonUtils
    ) {
        this.notificationRepository = notificationRepository;
        this.secureInboxCache = secureInboxCache;
        this.jsonUtils = jsonUtils;
    }

    @Override
    public String eventType() {
        return "SECURE_MESSAGE_ENCRYPTED";
    }

    @Override
    public void handle(Connection connection, OutboxEvent event) {
        SecureMessageEncryptedPayload payload = jsonUtils.fromJson(
                event.getPayload(),
                SecureMessageEncryptedPayload.class
        );

        Notification notification = Notification.secureMessageAvailable(
                payload.getRecipientId(),
                payload.getSecureMessageId()
        );

        notificationRepository.saveIfAbsent(connection, notification);

        secureInboxCache.evict(payload.getRecipientId());
    }
}