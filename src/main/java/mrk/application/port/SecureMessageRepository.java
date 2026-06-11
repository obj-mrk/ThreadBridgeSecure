package mrk.application.port;

import mrk.domain.model.SecureMessage;

import java.sql.Connection;

public interface SecureMessageRepository {
    long save(Connection connection, SecureMessage secureMessage);

    boolean existsByInboundMessageId(Connection connection, long inboundMessageId);
}
