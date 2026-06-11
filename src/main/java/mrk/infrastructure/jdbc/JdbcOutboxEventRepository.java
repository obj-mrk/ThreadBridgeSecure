package mrk.infrastructure.jdbc;

import mrk.application.port.OutboxEventRepository;
import mrk.domain.model.OutboxEvent;
import mrk.domain.value.OutboxEventStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcOutboxEventRepository implements OutboxEventRepository {

    @Override
    public long save(Connection connection, OutboxEvent event) {
        String sql = """
                INSERT INTO outbox_events (
                    event_type,
                    aggregate_type,
                    aggregate_id,
                    payload,
                    status
                )
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.getEventType());
            statement.setString(2, event.getAggregateType());
            statement.setLong(3, event.getAggregateId());
            statement.setString(4, event.getPayload());
            statement.setString(5, event.getStatus().name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new RuntimeException("Outbox event insert did not return id");
                }

                long id = resultSet.getLong("id");
                event.setId(id);
                return id;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save outbox event", exception);
        }
    }

    @Override
    public List<OutboxEvent> claimNewBatch(Connection connection, int batchSize) {
        String selectSql = """
            SELECT *
            FROM outbox_events
            WHERE status = 'NEW'
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT ?
            """;

        String updateSql = """
            UPDATE outbox_events
            SET status = 'PROCESSING',
                processing_started_at = CURRENT_TIMESTAMP,
                failure_reason = NULL
            WHERE id = ?
            """;

        try {
            List<OutboxEvent> events = new ArrayList<>();

            try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setInt(1, batchSize);

                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    while (resultSet.next()) {
                        events.add(mapOutboxEvent(resultSet));
                    }
                }
            }

            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                for (OutboxEvent event : events) {
                    updateStatement.setLong(1, event.getId());
                    updateStatement.addBatch();
                    event.setStatus(OutboxEventStatus.PROCESSING);
                }

                updateStatement.executeBatch();
            }

            return events;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not claim outbox events", exception);
        }
    }

    @Override
    public void markProcessed(Connection connection, long eventId) {
        String sql = """
            UPDATE outbox_events
            SET status = 'PROCESSED',
                processed_at = CURRENT_TIMESTAMP,
                failure_reason = NULL
            WHERE id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not mark outbox event as PROCESSED", exception);
        }
    }

    @Override
    public void markFailed(Connection connection, long eventId, String failureReason) {
        String sql = """
            UPDATE outbox_events
            SET status = 'FAILED',
                failure_reason = ?
            WHERE id = ?
              AND status <> 'PROCESSED'
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, failureReason);
            statement.setLong(2, eventId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not mark outbox event as FAILED", exception);
        }
    }

    private OutboxEvent mapOutboxEvent(ResultSet resultSet) throws SQLException {
        OutboxEvent event = new OutboxEvent();

        event.setId(resultSet.getLong("id"));
        event.setEventType(resultSet.getString("event_type"));
        event.setAggregateType(resultSet.getString("aggregate_type"));
        event.setAggregateId(resultSet.getLong("aggregate_id"));
        event.setPayload(resultSet.getString("payload"));
        event.setStatus(OutboxEventStatus.valueOf(resultSet.getString("status")));
        event.setFailureReason(resultSet.getString("failure_reason"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            event.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp processingStartedAt = resultSet.getTimestamp("processing_started_at");
        if (processingStartedAt != null) {
            event.setProcessingStartedAt(processingStartedAt.toLocalDateTime());
        }

        Timestamp processedAt = resultSet.getTimestamp("processed_at");
        if (processedAt != null) {
            event.setProcessedAt(processedAt.toLocalDateTime());
        }

        return event;
    }
}