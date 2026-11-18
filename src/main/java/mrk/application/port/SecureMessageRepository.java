package mrk.application.port;

import mrk.application.result.ExpiredBatchResult;
import mrk.application.result.SecureInboxItem;
import mrk.domain.model.SecureMessage;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface SecureMessageRepository {
    long save(Connection connection, SecureMessage message);

    Optional<Long> findIdByInboundMessageId(Connection connection, long inboundMessageId);

    List<SecureInboxItem> findInboxByRecipientId(Connection connection, long recipientId);

    Optional<SecureMessage> findByIdForRecipientForUpdate(Connection connection, long messageId, long recipientId);

    Optional<SecureMessage> findByIdForRecipient(Connection connection, long messageId, long recipientId);

    ExpiredBatchResult markExpiredBatch(Connection connection, int batchSize);

    void markRead(Connection connection, long messageId);

    void markDestroyed(Connection connection, long messageId);

    void markExpired(Connection connection, long messageId);
}