package mrk.bootstrap;

import com.sun.net.httpserver.HttpServer;
import mrk.application.port.*;
import mrk.application.usecase.*;
import mrk.config.AppConfig;
import mrk.http.HttpServerFactory;
import mrk.http.handler.*;
import mrk.infrastructure.crypto.JcaHybridEncryptionService;
import mrk.infrastructure.crypto.JcaKeyManagementService;
import mrk.infrastructure.crypto.RsaPemKeyParser;
import mrk.infrastructure.event.EventDispatcher;
import mrk.infrastructure.event.EventListener;
import mrk.infrastructure.event.listener.SecureMessageEncryptedListener;
import mrk.infrastructure.jdbc.*;
import mrk.infrastructure.worker.CryptoWorkerPool;
import mrk.infrastructure.worker.InboundSecureMessagePoller;
import mrk.infrastructure.worker.OutboxEventPoller;
import mrk.infrastructure.worker.OutboxEventWorkerPool;
import mrk.utils.JsonUtils;
import mrk.utils.KeyFingerprintCalculator;

import java.io.IOException;
import java.util.List;

public class DependencyFactory {
    private final AppConfig appConfig;

    public DependencyFactory(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public Application createApplication() throws IOException {
        ConnectionFactory connectionFactory = new ConnectionFactory(appConfig);
        DatabaseHealthChecker databaseHealthChecker = new DatabaseHealthChecker(connectionFactory);
        JsonUtils jsonUtils = new JsonUtils();

        TransactionManager transactionManager = new JdbcTransactionManager(connectionFactory);


        UserRepository userRepository = new JdbcUserRepository();
        UserKeyRepository userKeyRepository = new JdbcUserKeyRepository();
        InboundSecureMessageRepository inboundSecureMessageRepository =
                new JdbcInboundSecureMessageRepository();

        SecureMessageRepository secureMessageRepository = new JdbcSecureMessageRepository();
        OutboxEventRepository outboxEventRepository = new JdbcOutboxEventRepository();

        NotificationRepository notificationRepository = new JdbcNotificationRepository();

        KeyManagementService keyManagementService = new JcaKeyManagementService(
                new RsaPemKeyParser()
        );

        HybridEncryptionService hybridEncryptionService = new JcaHybridEncryptionService();

        KeyFingerprintCalculator keyFingerprintCalculator = new KeyFingerprintCalculator();

        RegisterUserUseCase registerUserUseCase = new RegisterUserUseCase(
                transactionManager,
                userRepository
        );

        RegisterUserKeyUseCase registerUserKeyUseCase = new RegisterUserKeyUseCase(
                transactionManager,
                userRepository,
                userKeyRepository,
                keyFingerprintCalculator
        );

        AcceptSecureMessageUseCase acceptSecureMessageUseCase = new AcceptSecureMessageUseCase(
                transactionManager,
                userRepository,
                inboundSecureMessageRepository,
                appConfig.getMaxSecureMessageLength(),
                appConfig.getMinMessageTtlSeconds(),
                appConfig.getMaxMessageTtlSeconds()
        );

        ProcessSecureMessageUseCase processSecureMessageUseCase = new ProcessSecureMessageUseCase(
                transactionManager,
                inboundSecureMessageRepository,
                secureMessageRepository,
                outboxEventRepository,
                userRepository,
                userKeyRepository,
                keyManagementService,
                hybridEncryptionService
        );

        GetSecureInboxUseCase getSecureInboxUseCase = new GetSecureInboxUseCase(
                transactionManager,
                secureMessageRepository
        );

        DecryptSecureMessageUseCase decryptSecureMessageUseCase = new DecryptSecureMessageUseCase(
                transactionManager,
                secureMessageRepository,
                userKeyRepository,
                keyManagementService,
                hybridEncryptionService
        );

        CryptoWorkerPool cryptoWorkerPool = new CryptoWorkerPool(
                appConfig.getCryptoWorkerThreads(),
                processSecureMessageUseCase
        );

        InboundSecureMessagePoller inboundSecureMessagePoller = new InboundSecureMessagePoller(
                transactionManager,
                inboundSecureMessageRepository,
                cryptoWorkerPool,
                appConfig.getInboundPollBatchSize(),
                appConfig.getInboundPollDelayMillis()
        );

        EventListener secureMessageEncryptedListener = new SecureMessageEncryptedListener(
                notificationRepository,
                jsonUtils
        );

        EventDispatcher eventDispatcher = new EventDispatcher(List.of(
                secureMessageEncryptedListener
        ));

        OutboxEventWorkerPool outboxEventWorkerPool = new OutboxEventWorkerPool(
                appConfig.getOutboxWorkerThreads(),
                transactionManager,
                outboxEventRepository,
                eventDispatcher
        );

        OutboxEventPoller outboxEventPoller = new OutboxEventPoller(
                transactionManager,
                outboxEventRepository,
                outboxEventWorkerPool,
                appConfig.getOutboxPollBatchSize(),
                appConfig.getOutboxPollDelayMillis()
        );

        HealthHandler healthHandler = new HealthHandler();
        UserHandler userHandler = new UserHandler(
                jsonUtils,
                registerUserUseCase);

        UserKeyHandler userKeyHandler = new UserKeyHandler(
                jsonUtils,
                registerUserKeyUseCase);

        SecureMessageWebhookHandler secureMessageWebhookHandler = new SecureMessageWebhookHandler(
                jsonUtils,
                acceptSecureMessageUseCase);

        SecureInboxHandler secureInboxHandler = new SecureInboxHandler(
                jsonUtils,
                getSecureInboxUseCase
        );

        DecryptSecureMessageHandler decryptSecureMessageHandler = new DecryptSecureMessageHandler(
                jsonUtils,
                decryptSecureMessageUseCase
        );

        HttpServerFactory httpServerFactory = new HttpServerFactory(
                appConfig,
                healthHandler,
                userHandler,
                userKeyHandler,
                secureMessageWebhookHandler,
                secureInboxHandler,
                decryptSecureMessageHandler
        );

        HttpServer httpServer = httpServerFactory.create();

        return new Application(
                httpServer,
                databaseHealthChecker,
                inboundSecureMessagePoller,
                cryptoWorkerPool,
                outboxEventPoller,
                outboxEventWorkerPool
        );
    }
}