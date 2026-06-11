package mrk.http;

import com.sun.net.httpserver.HttpServer;
import mrk.config.AppConfig;
import mrk.http.handler.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpServerFactory {
    private final AppConfig appConfig;
    private final HealthHandler healthHandler;
    private final UserHandler userHandler;
    private final UserKeyHandler userKeyHandler;
    private final SecureMessageWebhookHandler secureMessageWebhookHandler;
    private final SecureInboxHandler secureInboxHandler;
    private final DecryptSecureMessageHandler decryptSecureMessageHandler;

    public HttpServerFactory(
            AppConfig appConfig,
            HealthHandler healthHandler,
            UserHandler userHandler,
            UserKeyHandler userKeyHandler,
            SecureMessageWebhookHandler secureMessageWebhookHandler,
            SecureInboxHandler secureInboxHandler,
            DecryptSecureMessageHandler decryptSecureMessageHandler
    ) {
        this.appConfig = appConfig;
        this.healthHandler = healthHandler;
        this.userHandler = userHandler;
        this.userKeyHandler = userKeyHandler;
        this.secureMessageWebhookHandler = secureMessageWebhookHandler;
        this.secureInboxHandler = secureInboxHandler;
        this.decryptSecureMessageHandler = decryptSecureMessageHandler;
    }

    public HttpServer create() throws IOException {
        InetSocketAddress address = new InetSocketAddress(
                appConfig.getServerHost(),
                appConfig.getServerPort()
        );

        HttpServer server = HttpServer.create(address, appConfig.getBacklog());

        server.createContext("/health", healthHandler);
        server.createContext("/users", userHandler);
        server.createContext("/users/", userKeyHandler);
        server.createContext("/webhook/secure-messages", secureMessageWebhookHandler);

        server.createContext("/me/secure-messages", secureInboxHandler);
        server.createContext("/secure-messages/", decryptSecureMessageHandler);

        server.setExecutor(Executors.newFixedThreadPool(appConfig.getHttpWorkerThreads()));

        return server;
    }
}
