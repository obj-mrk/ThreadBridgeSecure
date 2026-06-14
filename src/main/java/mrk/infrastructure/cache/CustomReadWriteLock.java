package mrk.infrastructure.cache;

public class CustomReadWriteLock {
    private int readers;
    private int writers;
    private int writeRequests;

    public synchronized void lockRead() {
        while (writers > 0 || writeRequests > 0) {
            await();
        }
        readers++;
    }

    public synchronized void unlockRead() {
        if (readers <= 0) {
            throw new IllegalStateException("No active readers");
        }

        readers--;
        notifyAll();
    }

    public synchronized void lockWrite() {
        writeRequests++;

        while (readers > 0 || writers > 0) {
            await();
        }

        writeRequests--;
        writers++;
    }

    public synchronized void unlockWrite() {
        if (writers <= 0) {
            throw new IllegalStateException("No active writer");
        }

        writers--;
        notifyAll();
    }

    private void await() {
        try {
            wait();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted while waiting for lock", exception);
        }
    }
}