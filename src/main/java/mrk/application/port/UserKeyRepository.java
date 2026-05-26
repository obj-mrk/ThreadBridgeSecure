package mrk.application.port;

import mrk.domain.model.UserKey;

import java.sql.Connection;
import java.util.Optional;

public interface UserKeyRepository {
    long save(Connection connection, UserKey userKey);

    Optional<UserKey> findActiveByUserId(Connection connection, long userId);

    boolean existsByFingerprint(Connection connection, String keyFingerprint);

    void revokeActiveKeysByUserId(Connection connection, long userId);
}