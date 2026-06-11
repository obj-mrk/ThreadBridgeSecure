package mrk.application.port;

import mrk.domain.model.Notification;

import java.sql.Connection;

public interface NotificationRepository {
    long saveIfAbsent(Connection connection, Notification notification);
}