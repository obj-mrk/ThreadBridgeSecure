package mrk.config;

public class AppConfig {

    private final int serverPort = 8080;
    private final int backlog = 100;
    private final String serverHost = "localhost";
    private final String applicationName = "ThreadBridge Secure";

    private final String databaseUrl = "jdbc:postgresql://localhost:5432/threadbridge";
    private final String databaseUser = "threadbridge_user";
    private final String databasePassword = "threadbridge_password";

    public String getApplicationName() {
        return applicationName;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getBacklog() {
        return backlog;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }
}
