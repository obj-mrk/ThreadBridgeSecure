package mrk.bootstrap;

import com.sun.net.httpserver.HttpServer;
import mrk.infrastrucure.jdbc.DatabaseHealthChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final HttpServer server;
    private final DatabaseHealthChecker databaseHealthChecker;

    public Application(HttpServer server, DatabaseHealthChecker databaseHealthChecker) {
        this.server = server;
        this.databaseHealthChecker = databaseHealthChecker;
    }

    public void start() {
        if (!databaseHealthChecker.isDatabaseAvailable()) {
            throw new IllegalStateException("Database connection is not available");
        }

        log.info("Database connection is valid");

        server.start();
    }

    public void stop() {
        server.stop(1);
    }

}
