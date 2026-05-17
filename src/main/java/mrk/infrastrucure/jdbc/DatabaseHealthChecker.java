package mrk.infrastrucure.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseHealthChecker {
    private final ConnectionFactory connectionFactory;

    public DatabaseHealthChecker(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public boolean isDatabaseAvailable() {
        try (Connection connection = connectionFactory.createConnection()) {
            return connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
