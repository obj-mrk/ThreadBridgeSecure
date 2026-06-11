package mrk.infrastructure.jdbc;

import mrk.application.port.SecureMessageRepository;
import mrk.application.result.SecureInboxItem;
import mrk.domain.model.SecureMessage;
import mrk.domain.value.SecureMessageStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Override
    public List<SecureInboxItem> findInboxByRecipientId(Connection connection, long recipientId) {
        String sql = """
            SELECT
                id,
                sender_id,
                status,
                one_time,
                created_at,
                expires_at
            FROM secure_messages
            WHERE recipient_id = ?
              AND status <> 'DESTROYED'
            ORDER BY created_at DESC
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, recipientId);

            List<SecureInboxItem> items = new ArrayList<>();

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new SecureInboxItem(
                            resultSet.getLong("id"),
                            resultSet.getLong("sender_id"),
                            SecureMessageStatus.valueOf(resultSet.getString("status")),
                            resultSet.getBoolean("one_time"),
                            resultSet.getTimestamp("created_at").toLocalDateTime(),
                            resultSet.getTimestamp("expires_at").toLocalDateTime()
                    ));
                }
            }

            return items;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load secure inbox", exception);
        }
    }

    @Override
    public Optional<SecureMessage> findByIdForRecipientForUpdate(
            Connection connection,
            long messageId,
            long recipientId
    ) {
        String sql = """
            SELECT *
            FROM secure_messages
            WHERE id = ?
              AND recipient_id = ?
            FOR UPDATE
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            statement.setLong(2, recipientId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapSecureMessage(resultSet));
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load secure message for decrypt", exception);
        }
    }

    @Override
    public void markRead(Connection connection, long messageId) {
        String sql = """
            UPDATE secure_messages
            SET status = 'READ',
                read_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not mark secure message as READ", exception);
        }
    }

    @Override
    public void markDestroyed(Connection connection, long messageId) {
        String sql = """
            UPDATE secure_messages
            SET status = 'DESTROYED',
                destroyed_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not mark secure message as DESTROYED", exception);
        }
    }

    private SecureMessage mapSecureMessage(ResultSet resultSet) throws SQLException {
        SecureMessage message = new SecureMessage();

        message.setId(resultSet.getLong("id"));
        message.setInboundMessageId(resultSet.getLong("inbound_message_id"));
        message.setSenderId(resultSet.getLong("sender_id"));
        message.setRecipientId(resultSet.getLong("recipient_id"));
        message.setRecipientKeyId(resultSet.getLong("recipient_key_id"));
        message.setEncryptedPayload(resultSet.getString("encrypted_payload"));
        message.setEncryptedContentKey(resultSet.getString("encrypted_content_key"));
        message.setNonce(resultSet.getString("nonce"));
        message.setAlgorithm(resultSet.getString("algorithm"));
        message.setStatus(SecureMessageStatus.valueOf(resultSet.getString("status")));
        message.setOneTime(resultSet.getBoolean("one_time"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            message.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp expiresAt = resultSet.getTimestamp("expires_at");
        if (expiresAt != null) {
            message.setExpiresAt(expiresAt.toLocalDateTime());
        }

        Timestamp readAt = resultSet.getTimestamp("read_at");
        if (readAt != null) {
            message.setReadAt(readAt.toLocalDateTime());
        }

        Timestamp destroyedAt = resultSet.getTimestamp("destroyed_at");
        if (destroyedAt != null) {
            message.setDestroyedAt(destroyedAt.toLocalDateTime());
        }

        return message;
    }

}