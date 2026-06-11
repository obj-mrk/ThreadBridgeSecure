package mrk.infrastructure.event;

import mrk.domain.model.OutboxEvent;

import java.sql.Connection;

public interface EventListener {
    String eventType();

    void handle(Connection connection, OutboxEvent event);
}