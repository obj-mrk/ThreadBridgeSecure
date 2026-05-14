package mrk.http;

import com.sun.net.httpserver.HttpServer;
import mrk.config.AppConfig;
import mrk.http.handler.HealthHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerFactory {
    private final AppConfig appConfig;

    public HttpServerFactory(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public HttpServer create() throws IOException {
        InetSocketAddress address = new InetSocketAddress(
                appConfig.getServerHost(),
                appConfig.getServerPort()
        );

        HttpServer server = HttpServer.create(address, appConfig.getBacklog());

        server.createContext("/health", new HealthHandler());

        return server;
    }
}
