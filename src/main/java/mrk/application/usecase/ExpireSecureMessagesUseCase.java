package mrk.application.usecase;

import mrk.application.port.AuditEventRepository;
import mrk.application.port.OutboxEventRepository;
import mrk.application.port.SecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.service.SecureMessageAuditFactory;
import mrk.application.service.SecureMessageOutboxFactory;

public class ExpireSecureMessagesUseCase {

    private final TransactionManager transactionManager;
    private final SecureMessageRepository secureMessageRepository;
    private final AuditEventRepository auditEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final SecureMessageAuditFactory auditFactory;
    private final SecureMessageOutboxFactory outboxFactory;

    public ExpireSecureMessagesUseCase(
            TransactionManager transactionManager,
            SecureMessageRepository secureMessageRepository,
            AuditEventRepository auditEventRepository,
            OutboxEventRepository outboxEventRepository,
            SecureMessageAuditFactory auditFactory,
            SecureMessageOutboxFactory outboxFactory
    ) {
        this.transactionManager = transactionManager;
        this.secureMessageRepository = secureMessageRepository;
        this.auditEventRepository = auditEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditFactory = auditFactory;
        this.outboxFactory = outboxFactory;
    }

    public int expireBatch(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        return transactionManager.execute(connection -> {
            int expiredCount = secureMessageRepository.markExpiredBatch(connection, batchSize);

            if (expiredCount > 0) {
                auditEventRepository.save(
                        connection,
                        auditFactory.messagesExpired(expiredCount)
                );

                outboxEventRepository.save(
                        connection,
                        outboxFactory.messagesExpired(expiredCount)
                );
            }

            return expiredCount;
        });
    }
}