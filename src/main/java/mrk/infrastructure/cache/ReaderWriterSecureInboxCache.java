package mrk.infrastructure.cache;

import mrk.application.port.SecureInboxCache;
import mrk.application.result.SecureInboxItem;
import mrk.domain.value.SecureMessageStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReaderWriterSecureInboxCache implements SecureInboxCache {

    private final CustomReadWriteLock lock = new CustomReadWriteLock();
    private final Map<Long, List<SecureInboxItem>> inboxByRecipientId = new HashMap<>();

    @Override
    public Optional<List<SecureInboxItem>> get(long recipientId) {
        lock.lockRead();
        try {
            List<SecureInboxItem> cachedItems = inboxByRecipientId.get(recipientId);

            if (cachedItems == null) {
                return Optional.empty();
            }

            return Optional.of(copy(cachedItems));
        } finally {
            lock.unlockRead();
        }
    }

    @Override
    public void put(long recipientId, List<SecureInboxItem> items) {
        lock.lockWrite();
        try {
            inboxByRecipientId.put(recipientId, normalize(items));
        } finally {
            lock.unlockWrite();
        }
    }

    @Override
    public void upsert(long recipientId, SecureInboxItem item) {
        lock.lockWrite();
        try {
            List<SecureInboxItem> items = new ArrayList<>(
                    inboxByRecipientId.getOrDefault(recipientId, List.of())
            );

            items.removeIf(existing -> existing.getMessageId() == item.getMessageId());
            items.add(item);

            inboxByRecipientId.put(recipientId, normalize(items));
        } finally {
            lock.unlockWrite();
        }
    }

    @Override
    public void updateStatus(long recipientId, long messageId, String status) {
        lock.lockWrite();
        try {
            List<SecureInboxItem> items = inboxByRecipientId.get(recipientId);

            if (items == null) {
                return;
            }

            SecureMessageStatus parsedStatus = SecureMessageStatus.valueOf(status);
            List<SecureInboxItem> updated = new ArrayList<>();

            for (SecureInboxItem item : items) {
                if (item.getMessageId() == messageId) {
                    updated.add(new SecureInboxItem(
                            item.getMessageId(),
                            item.getSenderId(),
                            parsedStatus,
                            item.isOneTime(),
                            item.getCreatedAt(),
                            item.getExpiresAt()
                    ));
                } else {
                    updated.add(item);
                }
            }

            inboxByRecipientId.put(recipientId, normalize(updated));
        } finally {
            lock.unlockWrite();
        }
    }

    @Override
    public void evict(long recipientId) {
        lock.lockWrite();
        try {
            inboxByRecipientId.remove(recipientId);
        } finally {
            lock.unlockWrite();
        }
    }

    @Override
    public void clear() {
        lock.lockWrite();
        try {
            inboxByRecipientId.clear();
        } finally {
            lock.unlockWrite();
        }
    }

    private List<SecureInboxItem> normalize(List<SecureInboxItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(SecureInboxItem::getCreatedAt).reversed())
                .toList();
    }

    private List<SecureInboxItem> copy(List<SecureInboxItem> items) {
        return new ArrayList<>(items);
    }
}