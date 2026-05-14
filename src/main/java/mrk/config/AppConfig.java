package mrk.config;

public class AppConfig {

    private final int serverPort = 8080;
    private final int backlog = 100;
    private final String serverHost = "localhost";
    private final String applicationName = "ThreadBridge Secure";

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
}
