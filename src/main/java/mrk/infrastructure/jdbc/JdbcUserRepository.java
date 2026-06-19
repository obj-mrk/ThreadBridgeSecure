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
                SELECT id, username, created_at, status
                FROM users
                WHERE id = ?
                """;

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

    private User mapUser(ResultSet rs) throws SQLException {
        return User.restore(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                UserStatus.valueOf(rs.getString("status"))
        );
    }
}