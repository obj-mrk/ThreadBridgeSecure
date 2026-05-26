package mrk.infrastructure.jdbc;

import mrk.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    private final AppConfig appConfig;

    public ConnectionFactory(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(
                appConfig.getDatabaseUrl(),
                appConfig.getDatabaseUser(),
                appConfig.getDatabasePassword()
        );
    }
}
