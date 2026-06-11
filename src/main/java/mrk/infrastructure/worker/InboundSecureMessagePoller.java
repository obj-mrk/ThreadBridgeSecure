package mrk.infrastructure.worker;

import mrk.application.port.InboundSecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.domain.model.InboundSecureMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InboundSecureMessagePoller implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(InboundSecureMessagePoller.class);

    private final TransactionManager transactionManager;
    private final InboundSecureMessageRepository inboundRepository;
    private final CryptoWorkerPool cryptoWorkerPool;
    private final ScheduledExecutorService scheduler;
    private final int batchSize;
    private final long delayMillis;

    public InboundSecureMessagePoller(
            TransactionManager transactionManager,
            InboundSecureMessageRepository inboundRepository,
            CryptoWorkerPool cryptoWorkerPool,
            int batchSize,
            long delayMillis
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        if (delayMillis <= 0) {
            throw new IllegalArgumentException("Delay millis must be positive");
        }

        this.transactionManager = transactionManager;
        this.inboundRepository = inboundRepository;
        this.cryptoWorkerPool = cryptoWorkerPool;
        this.batchSize = batchSize;
        this.delayMillis = delayMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("inbound-poller")
        );
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(
                this::pollSafely,
                0,
                delayMillis,
                TimeUnit.MILLISECONDS
        );
    }

    private void pollSafely() {
        try {
            poll();
        } catch (RuntimeException exception) {
            // Нельзя позволить одному сбою БД убить ScheduledExecutor.
            // Следующий polling cycle должен продолжить работу.
            log.warn("Inbound polling failed: {}", exception.getMessage());
        }
    }

    private void poll() {
        List<InboundSecureMessage> claimedMessages = transactionManager.execute(connection ->
                inboundRepository.claimReceivedBatch(connection, batchSize)
        );

        for (InboundSecureMessage message : claimedMessages) {
            cryptoWorkerPool.submit(message.getId());
        }

        if (!claimedMessages.isEmpty()) {
            log.info("Claimed {} inbound secure message(s)", claimedMessages.size());
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Inbound poller did not stop gracefully, forcing shutdown");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }
}