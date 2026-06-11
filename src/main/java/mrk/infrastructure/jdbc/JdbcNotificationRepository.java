package mrk.infrastructure.jdbc;

import mrk.application.port.NotificationRepository;
import mrk.domain.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcNotificationRepository implements NotificationRepository {

    @Override
    public long saveIfAbsent(Connection connection, Notification notification) {
        String sql = """
                INSERT INTO notifications (
                    user_id,
                    secure_message_id,
                    type,
                    status,
                    text
                )
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (secure_message_id, type) DO NOTHING
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, notification.getUserId());
            statement.setLong(2, notification.getSecureMessageId());
            statement.setString(3, notification.getType().name());
            statement.setString(4, notification.getStatus().name());
            statement.setString(5, notification.getText());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    notification.setId(id);
                    return id;
                }

                return -1;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save notification", exception);
        }
    }
}