package mrk.infrastructure.worker;

import mrk.application.command.ProcessSecureMessageCommand;
import mrk.application.usecase.ProcessSecureMessageUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CryptoWorkerPool implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(CryptoWorkerPool.class);

    private final ExecutorService executor;
    private final ProcessSecureMessageUseCase processSecureMessageUseCase;

    public CryptoWorkerPool(
            int workerCount,
            ProcessSecureMessageUseCase processSecureMessageUseCase
    ) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be positive");
        }

        this.executor = Executors.newFixedThreadPool(
                workerCount,
                new NamedThreadFactory("crypto-worker")
        );
        this.processSecureMessageUseCase = processSecureMessageUseCase;
    }

    public void submit(long inboundMessageId) {
        executor.submit(() -> {
            try {
                processSecureMessageUseCase.process(
                        new ProcessSecureMessageCommand(inboundMessageId)
                );
            } catch (RuntimeException exception) {
                // Ошибка уже фиксируется use case-ом через markFailedSafely.
                // Здесь оставляем технический log, чтобы worker не падал молча.
                log.warn(
                        "Failed to process inbound secure message id={}: {}",
                        inboundMessageId,
                        exception.getMessage()
                );
            }
        });
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