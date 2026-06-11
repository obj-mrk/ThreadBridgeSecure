package mrk.bootstrap;

import com.sun.net.httpserver.HttpServer;
import mrk.infrastructure.jdbc.DatabaseHealthChecker;
import mrk.infrastructure.worker.CryptoWorkerPool;
import mrk.infrastructure.worker.InboundSecureMessagePoller;
import mrk.infrastructure.worker.OutboxEventPoller;
import mrk.infrastructure.worker.OutboxEventWorkerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final HttpServer server;
    private final DatabaseHealthChecker databaseHealthChecker;
    private final InboundSecureMessagePoller inboundSecureMessagePoller;
    private final CryptoWorkerPool cryptoWorkerPool;
    private final OutboxEventPoller outboxEventPoller;
    private final OutboxEventWorkerPool outboxEventWorkerPool;

    public Application(
            HttpServer server,
            DatabaseHealthChecker databaseHealthChecker,
            InboundSecureMessagePoller inboundSecureMessagePoller,
            CryptoWorkerPool cryptoWorkerPool,
            OutboxEventPoller outboxEventPoller,
            OutboxEventWorkerPool outboxEventWorkerPool
    ) {
        this.server = server;
        this.databaseHealthChecker = databaseHealthChecker;
        this.inboundSecureMessagePoller = inboundSecureMessagePoller;
        this.cryptoWorkerPool = cryptoWorkerPool;
        this.outboxEventPoller = outboxEventPoller;
        this.outboxEventWorkerPool = outboxEventWorkerPool;
    }

    public void start() {
        if (!databaseHealthChecker.isDatabaseAvailable()) {
            throw new IllegalStateException("Database connection is not available");
        }

        log.info("Database connection is valid");

        inboundSecureMessagePoller.start();
        log.info("Inbound secure message poller started");

        outboxEventPoller.start();
        log.info("Outbox event poller started");

        server.start();
        log.info("HTTP server started");
    }

    public void stop() {
        server.stop(1);
        inboundSecureMessagePoller.close();
        outboxEventPoller.close();
        cryptoWorkerPool.close();
        outboxEventWorkerPool.close();
    }
}