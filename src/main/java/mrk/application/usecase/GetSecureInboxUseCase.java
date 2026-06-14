package mrk.application.usecase;

import mrk.application.exception.ValidationException;
import mrk.application.port.SecureInboxCache;
import mrk.application.port.SecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.result.SecureInboxItem;

import java.util.List;

public class GetSecureInboxUseCase {

    private final TransactionManager transactionManager;
    private final SecureMessageRepository secureMessageRepository;
    private final SecureInboxCache secureInboxCache;

    public GetSecureInboxUseCase(
            TransactionManager transactionManager,
            SecureMessageRepository secureMessageRepository,
            SecureInboxCache secureInboxCache
    ) {
        this.transactionManager = transactionManager;
        this.secureMessageRepository = secureMessageRepository;
        this.secureInboxCache = secureInboxCache;
    }

    public List<SecureInboxItem> getInbox(long recipientId) {
        if (recipientId <= 0) {
            throw new ValidationException("Recipient id must be positive");
        }

        return secureInboxCache.get(recipientId)
                .orElseGet(() -> loadFromDatabaseAndCache(recipientId));
    }

    private List<SecureInboxItem> loadFromDatabaseAndCache(long recipientId) {
        List<SecureInboxItem> items = transactionManager.execute(connection ->
                secureMessageRepository.findInboxByRecipientId(connection, recipientId)
        );

        secureInboxCache.put(recipientId, items);
        return items;
    }
}