package mrk.infrastructure.worker;

import mrk.application.command.ProcessSecureMessageCommand;
import mrk.application.usecase.ProcessSecureMessageUseCase;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class CryptoWorkerPoolTest {

    @Test
    void shouldApplyBackpressureWhenQueueIsFull() throws Exception {
        ProcessSecureMessageUseCase useCase = mock(ProcessSecureMessageUseCase.class);

        CountDownLatch firstTaskStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstTask = new CountDownLatch(1);
        AtomicReference<String> thirdTaskThread = new AtomicReference<>();

        doAnswer(invocation -> {
            ProcessSecureMessageCommand command = invocation.getArgument(0);

            if (command.getInboundMessageId() == 1) {
                firstTaskStarted.countDown();
                releaseFirstTask.await(5, TimeUnit.SECONDS);
            }

            if (command.getInboundMessageId() == 3) {
                thirdTaskThread.set(Thread.currentThread().getName());
            }

            return null;
        }).when(useCase).process(any(ProcessSecureMessageCommand.class));

        String testThread = Thread.currentThread().getName();

        try (CryptoWorkerPool workerPool = new CryptoWorkerPool(1, 1, useCase)) {
            workerPool.submit(1);

            assertTrue(
                    firstTaskStarted.await(5, TimeUnit.SECONDS)
            );

            // Первая задача выполняется, вторая занимает единственное место в очереди.
            workerPool.submit(2);

            // Очередь заполнена, поэтому третью задачу выполняет вызывающий поток.
            workerPool.submit(3);

            assertEquals(
                    testThread,
                    thirdTaskThread.get()
            );

            releaseFirstTask.countDown();
        } finally {
            releaseFirstTask.countDown();
        }
    }
}