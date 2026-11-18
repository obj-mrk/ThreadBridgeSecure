package mrk.infrastructure.worker;

import mrk.application.port.OutboxEventRepository;
import mrk.application.port.TransactionManager;
import mrk.domain.model.OutboxEvent;
import mrk.infrastructure.event.EventDispatcher;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxEventWorkerPoolTest {

    @Test
    void shouldApplyBackpressureWhenQueueIsFull() throws Exception {
        Connection connection = mock(Connection.class);
        TransactionManager transactionManager = immediateTransactionManager(connection);
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        EventDispatcher dispatcher = mock(EventDispatcher.class);

        CountDownLatch firstTaskStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstTask = new CountDownLatch(1);
        AtomicReference<String> thirdTaskThread = new AtomicReference<>();

        doAnswer(invocation -> {
            OutboxEvent event = invocation.getArgument(1);

            if (event.getId() == 1L) {
                firstTaskStarted.countDown();
                releaseFirstTask.await(5, TimeUnit.SECONDS);
            }

            if (event.getId() == 3L) {
                thirdTaskThread.set(Thread.currentThread().getName());
            }

            return null;
        }).when(dispatcher).dispatch(any(Connection.class), any(OutboxEvent.class));

        String testThread = Thread.currentThread().getName();

        try (OutboxEventWorkerPool workerPool = new OutboxEventWorkerPool(
                1,
                1,
                transactionManager,
                repository,
                dispatcher
        )) {
            workerPool.submit(event(1));

            assertTrue(firstTaskStarted.await(5, TimeUnit.SECONDS));

            workerPool.submit(event(2));
            workerPool.submit(event(3));

            assertEquals(testThread, thirdTaskThread.get());

            releaseFirstTask.countDown();
        } finally {
            releaseFirstTask.countDown();
        }
    }

    @Test
    void shouldRetryTemporaryFailure() throws Exception {
        Connection connection = mock(Connection.class);
        TransactionManager transactionManager = immediateTransactionManager(connection);
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        EventDispatcher dispatcher = mock(EventDispatcher.class);

        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch completed = new CountDownLatch(1);

        doAnswer(invocation -> {
            int attempt = attempts.incrementAndGet();

            if (attempt < 3) {
                throw new RuntimeException("Temporary failure");
            }

            completed.countDown();
            return null;
        }).when(dispatcher).dispatch(any(Connection.class), any(OutboxEvent.class));

        try (OutboxEventWorkerPool workerPool = new OutboxEventWorkerPool(
                1,
                1,
                transactionManager,
                repository,
                dispatcher
        )) {
            workerPool.submit(event(1));

            assertTrue(completed.await(5, TimeUnit.SECONDS));
        }

        assertEquals(3, attempts.get());

        verify(repository, times(1))
                .markProcessed(connection, 1L);

        verify(repository, never())
                .markFailed(any(), anyLong(), anyString());
    }

    @Test
    void shouldMarkEventFailedAfterThreeAttempts() {
        Connection connection = mock(Connection.class);
        TransactionManager transactionManager = immediateTransactionManager(connection);
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        EventDispatcher dispatcher = mock(EventDispatcher.class);

        doThrow(new RuntimeException("Permanent failure"))
                .when(dispatcher)
                .dispatch(any(Connection.class), any(OutboxEvent.class));

        try (OutboxEventWorkerPool workerPool = new OutboxEventWorkerPool(
                1,
                1,
                transactionManager,
                repository,
                dispatcher
        )) {
            workerPool.submit(event(1));
        }

        verify(dispatcher, times(3))
                .dispatch(any(Connection.class), any(OutboxEvent.class));

        verify(repository, never())
                .markProcessed(any(), anyLong());

        verify(repository, times(1))
                .markFailed(connection, 1L, "Permanent failure");
    }

    private TransactionManager immediateTransactionManager(Connection connection) {
        return new TransactionManager() {
            @Override
            public <T> T execute(TransactionCallback<T> callback) {
                try {
                    return callback.doInTransaction(connection);
                } catch (RuntimeException exception) {
                    throw exception;
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        };
    }

    private OutboxEvent event(long id) {
        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setEventType("TEST_EVENT");

        return event;
    }
}