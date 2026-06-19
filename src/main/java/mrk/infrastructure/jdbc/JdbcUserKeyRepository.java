package mrk.infrastructure.jdbc;

import mrk.application.port.UserKeyRepository;
import mrk.domain.model.UserKey;
import mrk.domain.value.UserKeyStatus;

import java.sql.*;
import java.time.LocalDateTime;
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

                return resultSet.getLong("id");
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

    private UserKey mapUserKey(ResultSet rs) throws SQLException {
        return UserKey.restore(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("public_key_pem"),
                rs.getString("encrypted_private_key_pem"),
                rs.getString("key_fingerprint"),
                rs.getString("algorithm"),
                UserKeyStatus.valueOf(rs.getString("status")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("activated_at")),
                toNullableLocalDateTime(rs.getTimestamp("revoked_at"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            throw new IllegalStateException("Required timestamp is null");
        }

        return timestamp.toLocalDateTime();
    }

    private LocalDateTime toNullableLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}