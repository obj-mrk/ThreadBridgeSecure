package mrk.infrastructure.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseHealthChecker {
    Logger log = LoggerFactory.getLogger(DatabaseHealthChecker.class);
    private final ConnectionFactory connectionFactory;

    public DatabaseHealthChecker(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public boolean isDatabaseAvailable() {
        try (Connection connection = connectionFactory.createConnection()) {
            return connection.isValid(2);
        } catch (SQLException e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return false;
        }
    }
}
