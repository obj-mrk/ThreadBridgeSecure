package mrk.infrastructure.jdbc;

import mrk.application.port.OutboxEventRepository;
import mrk.domain.model.OutboxEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}