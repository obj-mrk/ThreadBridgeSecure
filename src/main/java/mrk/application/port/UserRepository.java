package mrk.application.port;

import mrk.domain.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface UserRepository {
    long save(Connection connection, User user) throws SQLException;

    Optional<User> findById(Connection connection, long userId);

    boolean existsByUsername(Connection connection, String username);
}
