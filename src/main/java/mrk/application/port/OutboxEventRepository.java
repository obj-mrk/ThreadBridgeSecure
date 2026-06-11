package mrk.application.port;

import mrk.domain.model.OutboxEvent;

import java.sql.Connection;

public interface OutboxEventRepository {
    long save(Connection connection, OutboxEvent event);
}