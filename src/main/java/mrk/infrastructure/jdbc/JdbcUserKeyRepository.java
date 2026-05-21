package mrk.infrastructure.jdbc;

import mrk.application.port.UserKeyRepository;
import mrk.domain.model.UserKey;
import mrk.domain.value.UserKeyStatus;

import java.sql.*;
import java.util.Optional;

public class JdbcUserKeyRepository implements UserKeyRepository {
    @Override
    public long save(Connection connection, UserKey userKey) {
        String sql = """
                INSERT INTO user_keys (
                    user_id,
                    public_key_pem,
                    encrypted_private_key_pem,
                    key_fingerprint,
                    algorithm,
                    status,
                    activated_at,
                    revoked_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userKey.getUserId());
            statement.setString(2, userKey.getPublicKeyPem());
            statement.setString(3, userKey.getEncryptedPrivateKeyPem());
            statement.setString(4, userKey.getKeyFingerprint());
            statement.setString(5, userKey.getAlgorithm());
            statement.setString(6, userKey.getStatus().name());
            statement.setTimestamp(7, Timestamp.valueOf(userKey.getActivatedAt()));
            statement.setNull(8, Types.TIMESTAMP);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Creating user key failed, no id returned");
                }

                long id = resultSet.getLong("id");
                userKey.setId(id);
                return id;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save user key", exception);
        }
    }

    @Override
    public Optional<UserKey> findActiveByUserId(Connection connection, long userId) {
        String sql = """
                SELECT
                    id,
                    user_id,
                    public_key_pem,
                    encrypted_private_key_pem,
                    key_fingerprint,
                    algorithm,
                    status,
                    created_at,
                    activated_at,
                    revoked_at
                FROM user_keys
                WHERE user_id = ?
                  AND status = 'ACTIVE'
                ORDER BY activated_at DESC
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapUserKey(resultSet));
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not find active user key", exception);
        }
    }

    @Override
    public boolean existsByFingerprint(Connection connection, String keyFingerprint) {
        String sql = """
                SELECT 1
                FROM user_keys
                WHERE key_fingerprint = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, keyFingerprint);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not check key fingerprint existence", exception);
        }
    }

    @Override
    public void revokeActiveKeysByUserId(Connection connection, long userId) {
        String sql = """
                UPDATE user_keys
                SET status = 'REVOKED',
                    revoked_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not revoke active user keys", exception);
        }
    }

    private UserKey mapUserKey(ResultSet resultSet) throws SQLException {
        UserKey userKey = new UserKey();
        userKey.setId(resultSet.getLong("id"));
        userKey.setUserId(resultSet.getLong("user_id"));
        userKey.setPublicKeyPem(resultSet.getString("public_key_pem"));
        userKey.setEncryptedPrivateKeyPem(resultSet.getString("encrypted_private_key_pem"));
        userKey.setKeyFingerprint(resultSet.getString("key_fingerprint"));
        userKey.setAlgorithm(resultSet.getString("algorithm"));
        userKey.setStatus(UserKeyStatus.valueOf(resultSet.getString("status")));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            userKey.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp activatedAt = resultSet.getTimestamp("activated_at");
        if (activatedAt != null) {
            userKey.setActivatedAt(activatedAt.toLocalDateTime());
        }

        Timestamp revokedAt = resultSet.getTimestamp("revoked_at");
        if (revokedAt != null) {
            userKey.setRevokedAt(revokedAt.toLocalDateTime());
        }

        return userKey;
    }
}