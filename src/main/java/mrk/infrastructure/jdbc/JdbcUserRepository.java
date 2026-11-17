package mrk.infrastructure.jdbc;

import mrk.application.port.UserRepository;
import mrk.domain.model.User;
import mrk.domain.value.UserStatus;

import java.sql.*;
import java.util.Optional;

public class JdbcUserRepository implements UserRepository {

    @Override
    public long save(Connection connection, User user) {
        String sql = """
                INSERT INTO users (username, status)
                VALUES (?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getStatus().name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Creating user failed, no id returned");
                }

                return resultSet.getLong("id");
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save user", exception);
        }
    }

    @Override
    public Optional<User> findById(Connection connection, long userId) {
        String sql = """
                SELECT id,
                       username,
                       created_at,
                       status
                FROM users
                WHERE id = ?
                """;

        return findUser(connection, userId, sql);
    }

    @Override
    public Optional<User> findByIdForUpdate(Connection connection, long userId) {
        String sql = """
                SELECT id,
                       username,
                       created_at,
                       status
                FROM users
                WHERE id = ?
                FOR UPDATE
                """;

        return findUser(connection, userId, sql);
    }

    @Override
    public boolean existsByUsername(Connection connection, String username) {
        String sql = """
                SELECT 1
                FROM users
                WHERE username = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not check username existence", exception);
        }
    }

    private Optional<User> findUser(
            Connection connection,
            long userId,
            String sql
    ) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapUser(resultSet));
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not find user by id", exception);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return User.restore(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                UserStatus.valueOf(resultSet.getString("status"))
        );
    }
}