package mrk.bootstrap;

import com.sun.net.httpserver.HttpServer;
import mrk.infrastructure.jdbc.DatabaseHealthChecker;
import mrk.infrastructure.worker.CryptoWorkerPool;
import mrk.infrastructure.worker.InboundSecureMessagePoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final HttpServer server;
    private final DatabaseHealthChecker databaseHealthChecker;
    private final InboundSecureMessagePoller inboundSecureMessagePoller;
    private final CryptoWorkerPool cryptoWorkerPool;

    public Application(
            HttpServer server,
            DatabaseHealthChecker databaseHealthChecker,
            InboundSecureMessagePoller inboundSecureMessagePoller,
            CryptoWorkerPool cryptoWorkerPool
    ) {
        this.server = server;
        this.databaseHealthChecker = databaseHealthChecker;
        this.inboundSecureMessagePoller = inboundSecureMessagePoller;
        this.cryptoWorkerPool = cryptoWorkerPool;
    }

    public void start() {
        if (!databaseHealthChecker.isDatabaseAvailable()) {
            throw new IllegalStateException("Database connection is not available");
        }

        log.info("Database connection is valid");

        inboundSecureMessagePoller.start();
        log.info("Inbound secure message poller started");

        server.start();
        log.info("HTTP server started");
    }

    public void stop() {
        // Порядок важен:
        // 1. перестаём принимать HTTP;
        // 2. перестаём claim-ить новые inbound задачи;
        // 3. ждём завершения уже принятых crypto задач.
        server.stop(1);
        inboundSecureMessagePoller.close();
        cryptoWorkerPool.close();
    }
}