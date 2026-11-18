package mrk.application.port;

import mrk.domain.model.OutboxEvent;

import java.sql.Connection;
import java.util.List;

public interface OutboxEventRepository {
    long save(Connection connection, OutboxEvent event);

    List<OutboxEvent> claimProcessableBatch(Connection connection, int batchSize, int processingTimeoutSeconds);

    void markProcessed(Connection connection, long eventId);

    void markFailed(Connection connection, long eventId, String failureReason);
}