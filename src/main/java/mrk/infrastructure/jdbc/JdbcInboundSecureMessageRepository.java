package mrk.infrastructure.jdbc;

import mrk.application.port.InboundSecureMessageRepository;
import mrk.domain.model.InboundSecureMessage;
import mrk.domain.value.InboundMessageStatus;

import java.sql.*;
import java.util.Optional;

public class JdbcInboundSecureMessageRepository implements InboundSecureMessageRepository {

    @Override
    public long save(Connection connection, InboundSecureMessage message) {
        String sql = """
                INSERT INTO inbound_secure_messages (
                    request_id,
                    sender_id,
                    recipient_id,
                    plaintext_payload,
                    ttl_seconds,
                    one_time,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, message.getRequestId());
            statement.setLong(2, message.getSenderId());
            statement.setLong(3, message.getRecipientId());
            statement.setString(4, message.getPlaintextPayload());
            statement.setInt(5, message.getTtlSeconds());
            statement.setBoolean(6, message.isOneTime());
            statement.setString(7, message.getStatus().name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }

                throw new IllegalStateException("Inbound secure message was not inserted");
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save inbound secure message", exception);
        }
    }

    @Override
    public Optional<InboundSecureMessage> findByRequestId(Connection connection, String requestId) {
        String sql = """
                SELECT id,
                       request_id,
                       sender_id,
                       recipient_id,
                       plaintext_payload,
                       ttl_seconds,
                       one_time,
                       status,
                       failure_reason,
                       received_at,
                       processing_started_at,
                       processed_at
                FROM inbound_secure_messages
                WHERE request_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapInboundSecureMessage(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not find inbound secure message by request id", exception);
        }
    }

    @Override
    public boolean existsByRequestId(Connection connection, String requestId) {
        String sql = """
                SELECT 1
                FROM inbound_secure_messages
                WHERE request_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not check inbound secure message existence", exception);
        }
    }

    private InboundSecureMessage mapInboundSecureMessage(ResultSet resultSet) throws SQLException {
        InboundSecureMessage message = new InboundSecureMessage();

        message.setId(resultSet.getLong("id"));
        message.setRequestId(resultSet.getString("request_id"));
        message.setSenderId(resultSet.getLong("sender_id"));
        message.setRecipientId(resultSet.getLong("recipient_id"));
        message.setPlaintextPayload(resultSet.getString("plaintext_payload"));
        message.setTtlSeconds(resultSet.getInt("ttl_seconds"));
        message.setOneTime(resultSet.getBoolean("one_time"));
        message.setStatus(InboundMessageStatus.valueOf(resultSet.getString("status")));
        message.setFailureReason(resultSet.getString("failure_reason"));

        Timestamp receivedAt = resultSet.getTimestamp("received_at");
        if (receivedAt != null) {
            message.setReceivedAt(receivedAt.toLocalDateTime());
        }

        Timestamp processingStartedAt = resultSet.getTimestamp("processing_started_at");
        if (processingStartedAt != null) {
            message.setProcessingStartedAt(processingStartedAt.toLocalDateTime());
        }

        Timestamp processedAt = resultSet.getTimestamp("processed_at");
        if (processedAt != null) {
            message.setProcessedAt(processedAt.toLocalDateTime());
        }

        return message;
    }
}