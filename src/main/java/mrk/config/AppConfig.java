package mrk.config;

public class AppConfig {
    private final int serverPort = 8080;
    private final int backlog = 100;
    private final String serverHost = "localhost";
    private final String applicationName = "ThreadBridge Secure";

    private final String databaseUrl = getConfig(
            "THREADBRIDGE_DB_URL",
            "threadbridge.db.url",
            "jdbc:postgresql://localhost:5432/threadbridge"
    );

    private final String databaseUser = getConfig(
            "THREADBRIDGE_DB_USER",
            "threadbridge.db.user",
            "threadbridge_user"
    );

    private final String databasePassword = getConfig(
            "THREADBRIDGE_DB_PASSWORD",
            "threadbridge.db.password",
            null
    );



    private final int httpWorkerThreads = 8;
    
    private final int maxSecureMessageLength = 10_000;
    private final int minMessageTtlSeconds = 60;
    private final int maxMessageTtlSeconds = 86_400;

    private final int cryptoWorkerThreads = 4;
    private final int cryptoQueueCapacity = 8;

    private final int inboundPollBatchSize = 10;
    private final long inboundPollDelayMillis = 1_000;
    private final int inboundProcessingTimeoutSeconds = 30;

    private final int outboxWorkerThreads = 2;
    private final int outboxPollBatchSize = 10;
    private final int outboxPollDelayMillis = 1_000;

    private final int expiredCleanupBatchSize = 50;
    private final long expiredCleanupDelayMillis = 10_000;

    private String getConfig(String environmentName, String propertyName, String defaultValue) {
        String environmentValue = System.getenv(environmentName);

        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        String propertyValue = System.getProperty(propertyName);

        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        return defaultValue;
    }



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
        if (databasePassword == null || databasePassword.isBlank()) {
            throw new IllegalStateException(
                    "Database password is not configured. Set THREADBRIDGE_DB_PASSWORD"
            );
        }

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

    public int getOutboxWorkerThreads() {
        return outboxWorkerThreads;
    }

    public int getOutboxPollBatchSize() {
        return outboxPollBatchSize;
    }

    public int getOutboxPollDelayMillis() {
        return outboxPollDelayMillis;
    }

    public int getExpiredCleanupBatchSize() {
        return expiredCleanupBatchSize;
    }

    public long getExpiredCleanupDelayMillis() {
        return expiredCleanupDelayMillis;
    }

    public int getCryptoQueueCapacity() {return cryptoQueueCapacity;}

    public int getInboundProcessingTimeoutSeconds() {return inboundProcessingTimeoutSeconds;}
}
