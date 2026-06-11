package mrk.infrastructure.jdbc;

import mrk.application.port.SecureMessageRepository;
import mrk.domain.model.SecureMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcSecureMessageRepository implements SecureMessageRepository {

    @Override
    public long save(Connection connection, SecureMessage message) {
        String sql = """
                INSERT INTO secure_messages (
                    inbound_message_id,
                    sender_id,
                    recipient_id,
                    recipient_key_id,
                    encrypted_payload,
                    encrypted_content_key,
                    nonce,
                    algorithm,
                    status,
                    one_time,
                    expires_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, message.getInboundMessageId());
            statement.setLong(2, message.getSenderId());
            statement.setLong(3, message.getRecipientId());
            statement.setLong(4, message.getRecipientKeyId());
            statement.setString(5, message.getEncryptedPayload());
            statement.setString(6, message.getEncryptedContentKey());
            statement.setString(7, message.getNonce());
            statement.setString(8, message.getAlgorithm());
            statement.setString(9, message.getStatus().name());
            statement.setBoolean(10, message.isOneTime());
            statement.setObject(11, message.getExpiresAt());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new RuntimeException("Secure message insert did not return id");
                }

                long id = resultSet.getLong("id");
                message.setId(id);
                return id;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save secure message", exception);
        }
    }

    @Override
    public boolean existsByInboundMessageId(Connection connection, long inboundMessageId) {
        String sql = """
                SELECT 1
                FROM secure_messages
                WHERE inbound_message_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, inboundMessageId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not check secure message existence", exception);
        }
    }
}