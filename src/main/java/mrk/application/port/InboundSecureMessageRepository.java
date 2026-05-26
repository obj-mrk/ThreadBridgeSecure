package mrk.application.port;

import mrk.domain.model.InboundSecureMessage;

import java.sql.Connection;
import java.util.Optional;

public interface InboundSecureMessageRepository {
    long save(Connection connection, InboundSecureMessage message);

    Optional<InboundSecureMessage> findByRequestId(Connection connection, String requestId);

    boolean existsByRequestId(Connection connection, String requestId);
}