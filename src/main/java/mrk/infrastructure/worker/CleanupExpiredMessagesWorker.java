package mrk.infrastructure.worker;

import mrk.application.usecase.ExpireSecureMessagesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CleanupExpiredMessagesWorker implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(CleanupExpiredMessagesWorker.class);

    private final ScheduledExecutorService scheduler;
    private final ExpireSecureMessagesUseCase expireSecureMessagesUseCase;
    private final int batchSize;
    private final long delayMillis;

    public CleanupExpiredMessagesWorker(
            ExpireSecureMessagesUseCase expireSecureMessagesUseCase,
            int batchSize,
            long delayMillis
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        if (delayMillis <= 0) {
            throw new IllegalArgumentException("Delay millis must be positive");
        }

        this.expireSecureMessagesUseCase = expireSecureMessagesUseCase;
        this.batchSize = batchSize;
        this.delayMillis = delayMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("expired-cleanup")
        );
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(
                this::cleanupSafely,
                delayMillis,
                delayMillis,
                TimeUnit.MILLISECONDS
        );
    }

    private void cleanupSafely() {
        try {
            int expiredCount = expireSecureMessagesUseCase.expireBatch(batchSize);

            if (expiredCount > 0) {
                log.info("Expired {} secure message(s)", expiredCount);
            }
        } catch (RuntimeException exception) {
            // Один сбой cleanup не должен остановить scheduler.
            log.warn("Expired messages cleanup failed: {}", exception.getMessage());
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Expired cleanup worker did not stop gracefully, forcing shutdown");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }
}