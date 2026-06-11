package mrk.config;

public class AppConfig {

    private final int serverPort = 8080;
    private final int backlog = 100;
    private final String serverHost = "localhost";
    private final String applicationName = "ThreadBridge Secure";

    private final String databaseUrl = "jdbc:postgresql://localhost:5432/threadbridge";
    private final String databaseUser = "threadbridge_user";
    private final String databasePassword = "threadbridge_password";

    private final int httpWorkerThreads = 8;
    
    private final int maxSecureMessageLength = 10_000;
    private final int minMessageTtlSeconds = 60;
    private final int maxMessageTtlSeconds = 86_400;

    private final int cryptoWorkerThreads = 4;
    private final int inboundPollBatchSize = 10;
    private final long inboundPollDelayMillis = 1_000;

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

    public int getHttpWorkerThreads() {
        return httpWorkerThreads;
    }

    public int getMaxSecureMessageLength() {
        return maxSecureMessageLength;
    }

    public int getMinMessageTtlSeconds() {
        return minMessageTtlSeconds;
    }

    public int getMaxMessageTtlSeconds() {
        return maxMessageTtlSeconds;
    }

    public int getCryptoWorkerThreads() {
        return cryptoWorkerThreads;
    }

    public int getInboundPollBatchSize() {
        return inboundPollBatchSize;
    }

    public long getInboundPollDelayMillis() {
        return inboundPollDelayMillis;
    }
}
