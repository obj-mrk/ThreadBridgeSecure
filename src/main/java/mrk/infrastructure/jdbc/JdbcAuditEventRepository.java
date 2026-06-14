package mrk.infrastructure.jdbc;

import mrk.application.port.AuditEventRepository;
import mrk.domain.model.AuditEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcAuditEventRepository implements AuditEventRepository {

    @Override
    public long save(Connection connection, AuditEvent event) {
        String sql = """
                INSERT INTO audit_events (
                    actor_user_id,
                    event_type,
                    aggregate_type,
                    aggregate_id,
                    details
                )
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (event.getActorUserId() == null) {
                statement.setObject(1, null);
            } else {
                statement.setLong(1, event.getActorUserId());
            }

            statement.setString(2, event.getEventType().name());
            statement.setString(3, event.getAggregateType());

            if (event.getAggregateId() == null) {
                statement.setObject(4, null);
            } else {
                statement.setLong(4, event.getAggregateId());
            }

            statement.setString(5, event.getDetails());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new RuntimeException("Audit event insert did not return id");
                }

                long id = resultSet.getLong("id");
                event.setId(id);
                return id;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save audit event", exception);
        }
    }
}