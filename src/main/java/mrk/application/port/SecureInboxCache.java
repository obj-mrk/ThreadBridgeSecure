package mrk.application.port;

import mrk.application.result.SecureInboxItem;

import java.util.List;
import java.util.Optional;

public interface SecureInboxCache {

    Optional<List<SecureInboxItem>> get(long recipientId);

    void put(long recipientId, List<SecureInboxItem> items);

    void upsert(long recipientId, SecureInboxItem item);

    void updateStatus(long recipientId, long messageId, String status);

    void evict(long recipientId);

    void clear();
}