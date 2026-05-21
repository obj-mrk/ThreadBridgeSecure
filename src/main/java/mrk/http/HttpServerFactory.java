package mrk.http;

import com.sun.net.httpserver.HttpServer;
import mrk.config.AppConfig;
import mrk.http.handler.HealthHandler;
import mrk.http.handler.UserHandler;
import mrk.http.handler.UserKeyHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpServerFactory {
    private final AppConfig appConfig;
    private final HealthHandler healthHandler;
    private final UserHandler userHandler;
    private final UserKeyHandler userKeyHandler;

    public HttpServerFactory(AppConfig appConfig, HealthHandler healthHandler, UserHandler userHandler, UserKeyHandler userKeyHandler) {
        this.appConfig = appConfig;
        this.healthHandler = healthHandler;
        this.userHandler = userHandler;
        this.userKeyHandler = userKeyHandler;
    }

    public HttpServer create() throws IOException {
        InetSocketAddress address = new InetSocketAddress(
                appConfig.getServerHost(),
                appConfig.getServerPort()
        );

        HttpServer server = HttpServer.create(address, appConfig.getBacklog());
        server.setExecutor(Executors.newFixedThreadPool(appConfig.getHttpWorkerThreads()));

        server.createContext("/health", healthHandler);
        server.createContext("/users", userHandler);
        server.createContext("/users/", userKeyHandler);

        return server;
    }
}
