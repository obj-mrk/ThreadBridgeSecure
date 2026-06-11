package mrk.infrastructure.event.listener;

import mrk.application.port.NotificationRepository;
import mrk.domain.model.Notification;
import mrk.domain.model.OutboxEvent;
import mrk.infrastructure.event.EventListener;
import mrk.infrastructure.event.payload.SecureMessageEncryptedPayload;
import mrk.utils.JsonUtils;

import java.sql.Connection;

public class SecureMessageEncryptedListener implements EventListener {
    private final NotificationRepository notificationRepository;
    private final JsonUtils jsonUtils;

    public SecureMessageEncryptedListener(
            NotificationRepository notificationRepository,
            JsonUtils jsonUtils
    ) {
        this.notificationRepository = notificationRepository;
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

        // Listener должен быть идемпотентным.
        // Если outbox событие будет доставлено повторно после сбоя,
        // UNIQUE(secure_message_id, type) не даст создать дубль notification.
        notificationRepository.saveIfAbsent(connection, notification);
    }
}