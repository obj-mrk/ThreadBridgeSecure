package mrk.infrastructure.worker;

import mrk.application.port.OutboxEventRepository;
import mrk.application.port.TransactionManager;
import mrk.domain.model.OutboxEvent;
import mrk.infrastructure.event.EventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OutboxEventWorkerPool implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventWorkerPool.class);
    private static final int MAX_PROCESS_ATTEMPTS = 3;

    private final ThreadPoolExecutor executor;
    private final TransactionManager transactionManager;
    private final OutboxEventRepository outboxEventRepository;
    private final EventDispatcher eventDispatcher;

    public OutboxEventWorkerPool(int workerCount, int queueCapacity, TransactionManager transactionManager, OutboxEventRepository outboxEventRepository, EventDispatcher eventDispatcher) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be positive");
        }

        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be positive");
        }

        this.transactionManager = transactionManager;
        this.outboxEventRepository = outboxEventRepository;
        this.eventDispatcher = eventDispatcher;

        this.executor = new ThreadPoolExecutor(
                workerCount,
                workerCount,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new NamedThreadFactory("outbox-worker"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void submit(OutboxEvent event) {
        executor.execute(() -> processSafely(event));
    }

    private void processSafely(OutboxEvent event) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= MAX_PROCESS_ATTEMPTS; attempt++) {
            try {
                process(event);
                return;
            } catch (RuntimeException exception) {
                lastException = exception;
            }
        }

        markFailedSafely(event, lastException);

        log.warn(
                "Failed to dispatch outbox event id={}, type={} after {} attempts: {}",
                event.getId(),
                event.getEventType(),
                MAX_PROCESS_ATTEMPTS,
                lastException != null ? lastException.getMessage() : "unknown error"
        );
    }

    private void process(OutboxEvent event) {
        transactionManager.execute(connection -> {
            // Listener и PROCESSED фиксируются одной транзакцией.
            eventDispatcher.dispatch(connection, event);
            outboxEventRepository.markProcessed(connection, event.getId());

            return null;
        });
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
            // Если БД недоступна, событие останется PROCESSING и позже будет подобрано recovery.
        }
    }

    private String sanitizeFailureReason(RuntimeException exception) {
        if (exception == null) {
            return "Unknown error";
        }

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