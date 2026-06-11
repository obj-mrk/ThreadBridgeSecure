package mrk.infrastructure.worker;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private final String namePrefix;
    private final AtomicInteger counter = new AtomicInteger(1);

    public NamedThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(namePrefix + "-" + counter.getAndIncrement());

        // Не daemon: приложение должно завершаться через controlled shutdown,
        // а не через внезапное убийство фоновых задач.
        thread.setDaemon(false);

        return thread;
    }
}