package mrk.infrastructure.worker;

import mrk.application.command.ProcessSecureMessageCommand;
import mrk.application.usecase.ProcessSecureMessageUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CryptoWorkerPool implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(CryptoWorkerPool.class);

    private final ThreadPoolExecutor executor;
    private final ProcessSecureMessageUseCase processSecureMessageUseCase;

    public CryptoWorkerPool(int workerCount, int queueCapacity, ProcessSecureMessageUseCase processSecureMessageUseCase) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be positive");
        }

        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be positive");
        }

        this.processSecureMessageUseCase = processSecureMessageUseCase;

        this.executor = new ThreadPoolExecutor(
                workerCount,
                workerCount,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new NamedThreadFactory("crypto-worker"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void submit(long inboundMessageId) {
        executor.execute(() -> processSafely(inboundMessageId));
    }

    private void processSafely(long inboundMessageId) {
        try {
            processSecureMessageUseCase.process(
                    new ProcessSecureMessageCommand(inboundMessageId)
            );
        } catch (RuntimeException exception) {
            // Ошибка бизнес-обработки уже сохраняется внутри use case.
            log.warn(
                    "Failed to process inbound secure message id={}: {}",
                    inboundMessageId,
                    exception.getMessage()
            );
        }
    }

    @Override
    public void close() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Crypto worker pool did not stop gracefully, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}