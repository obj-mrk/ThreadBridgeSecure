package mrk.application.usecase;

import mrk.application.exception.ValidationException;
import mrk.application.port.SecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.result.SecureInboxItem;

import java.util.List;

public class GetSecureInboxUseCase {
    private final TransactionManager transactionManager;
    private final SecureMessageRepository secureMessageRepository;

    public GetSecureInboxUseCase(
            TransactionManager transactionManager,
            SecureMessageRepository secureMessageRepository
    ) {
        this.transactionManager = transactionManager;
        this.secureMessageRepository = secureMessageRepository;
    }

    public List<SecureInboxItem> getInbox(long recipientId) {
        if (recipientId <= 0) {
            throw new ValidationException("Recipient id must be positive");
        }

        return transactionManager.execute(connection ->
                secureMessageRepository.findInboxByRecipientId(connection, recipientId)
        );
    }
}