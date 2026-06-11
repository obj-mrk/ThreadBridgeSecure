package mrk.application.port;

import mrk.application.result.SecureInboxItem;
import mrk.domain.model.SecureMessage;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface SecureMessageRepository {
    long save(Connection connection, SecureMessage message);

    boolean existsByInboundMessageId(Connection connection, long inboundMessageId);

    List<SecureInboxItem> findInboxByRecipientId(Connection connection, long recipientId);

    Optional<SecureMessage> findByIdForRecipientForUpdate(
            Connection connection,
            long messageId,
            long recipientId
    );

    void markRead(Connection connection, long messageId);

    void markDestroyed(Connection connection, long messageId);
}