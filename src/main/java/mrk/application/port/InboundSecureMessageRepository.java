package mrk.application.port;

import mrk.domain.model.InboundSecureMessage;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface InboundSecureMessageRepository {

    Optional<Long> saveIfAbsent(Connection connection, InboundSecureMessage message);

    Optional<InboundSecureMessage> findByRequestId(Connection connection, String requestId);

    List<InboundSecureMessage> claimProcessableBatch(Connection connection, int batchSize, int processingTimeoutSeconds);

    Optional<InboundSecureMessage> findByIdForUpdate(Connection connection, long id);

    void markProcessedAndClearPlaintext(Connection connection, long id);

    void markFailed(Connection connection, long id, String failureReason);
}