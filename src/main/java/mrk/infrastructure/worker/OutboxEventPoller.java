package mrk.infrastructure.worker;

import mrk.application.port.OutboxEventRepository;
import mrk.application.port.TransactionManager;
import mrk.domain.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OutboxEventPoller implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventPoller.class);

    private final TransactionManager transactionManager;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventWorkerPool outboxEventWorkerPool;
    private final ScheduledExecutorService scheduler;
    private final int batchSize;
    private final long delayMillis;

    public OutboxEventPoller(
            TransactionManager transactionManager,
            OutboxEventRepository outboxEventRepository,
            OutboxEventWorkerPool outboxEventWorkerPool,
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
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventWorkerPool = outboxEventWorkerPool;
        this.batchSize = batchSize;
        this.delayMillis = delayMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("outbox-poller")
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
            log.warn("Outbox polling failed: {}", exception.getMessage());
        }
    }

    private void poll() {
        List<OutboxEvent> claimedEvents = transactionManager.execute(connection ->
                outboxEventRepository.claimNewBatch(connection, batchSize)
        );

        for (OutboxEvent event : claimedEvents) {
            outboxEventWorkerPool.submit(event);
        }

        if (!claimedEvents.isEmpty()) {
            log.info("Claimed {} outbox event(s)", claimedEvents.size());
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Outbox poller did not stop gracefully, forcing shutdown");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }
}