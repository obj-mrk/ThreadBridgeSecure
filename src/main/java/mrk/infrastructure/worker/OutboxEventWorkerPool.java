package mrk.infrastructure.worker;

import mrk.application.port.OutboxEventRepository;
import mrk.application.port.TransactionManager;
import mrk.domain.model.OutboxEvent;
import mrk.infrastructure.event.EventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OutboxEventWorkerPool implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventWorkerPool.class);

    private final ExecutorService executor;
    private final TransactionManager transactionManager;
    private final OutboxEventRepository outboxEventRepository;
    private final EventDispatcher eventDispatcher;

    public OutboxEventWorkerPool(
            int workerCount,
            TransactionManager transactionManager,
            OutboxEventRepository outboxEventRepository,
            EventDispatcher eventDispatcher
    ) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be positive");
        }

        this.executor = Executors.newFixedThreadPool(
                workerCount,
                new NamedThreadFactory("outbox-worker")
        );
        this.transactionManager = transactionManager;
        this.outboxEventRepository = outboxEventRepository;
        this.eventDispatcher = eventDispatcher;
    }

    public void submit(OutboxEvent event) {
        executor.submit(() -> processSafely(event));
    }

    private void processSafely(OutboxEvent event) {
        try {
            transactionManager.execute(connection -> {
                // Listener-ы и смена статуса outbox выполняются в одной транзакции.
                // Поэтому notification и PROCESSED либо фиксируются вместе, либо откатываются вместе.
                eventDispatcher.dispatch(connection, event);
                outboxEventRepository.markProcessed(connection, event.getId());
                return null;
            });
        } catch (RuntimeException exception) {
            markFailedSafely(event, exception);

            log.warn(
                    "Failed to dispatch outbox event id={}, type={}: {}",
                    event.getId(),
                    event.getEventType(),
                    exception.getMessage()
            );
        }
    }

    private void markFailedSafely(OutboxEvent event, RuntimeException exception) {
        try {
            transactionManager.execute(connection -> {
                outboxEventRepository.markFailed(
                        connection,
                        event.getId(),
                        sanitizeFailureReason(exception)
                );
                return null;
            });
        } catch (RuntimeException ignored) {
            // Не перебиваем первичную ошибку вторичной ошибкой фиксации FAILED.
        }
    }

    private String sanitizeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    @Override
    public void close() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Outbox worker pool did not stop gracefully, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}