package mrk.bootstrap;

import com.sun.net.httpserver.HttpServer;
import mrk.config.AppConfig;
import mrk.http.HttpServerFactory;
import mrk.infrastrucure.jdbc.ConnectionFactory;
import mrk.infrastrucure.jdbc.DatabaseHealthChecker;

import java.io.IOException;

public class DependencyFactory {
    private final AppConfig appConfig;

    public DependencyFactory(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public Application createApplication() throws IOException {
        HttpServerFactory httpServerFactory = new HttpServerFactory(appConfig);
        HttpServer httpServer = httpServerFactory.create();

        ConnectionFactory connectionFactory = new ConnectionFactory(appConfig);
        DatabaseHealthChecker databaseHealthChecker = new DatabaseHealthChecker(connectionFactory);

        return new Application(httpServer, databaseHealthChecker);
    }
}
