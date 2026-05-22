package mrk.bootstrap;

import com.sun.net.httpserver.HttpServer;
import mrk.application.port.InboundSecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserKeyRepository;
import mrk.application.port.UserRepository;
import mrk.application.usecase.AcceptSecureMessageUseCase;
import mrk.application.usecase.RegisterUserKeyUseCase;
import mrk.application.usecase.RegisterUserUseCase;
import mrk.config.AppConfig;
import mrk.http.HttpServerFactory;
import mrk.http.handler.HealthHandler;
import mrk.http.handler.SecureMessageWebhookHandler;
import mrk.http.handler.UserHandler;
import mrk.http.handler.UserKeyHandler;
import mrk.infrastructure.jdbc.*;
import mrk.utils.JsonUtils;
import mrk.utils.KeyFingerprintCalculator;

import java.io.IOException;

public class DependencyFactory {
    private final AppConfig appConfig;

    public DependencyFactory(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public Application createApplication() throws IOException {
        ConnectionFactory connectionFactory = new ConnectionFactory(appConfig);
        DatabaseHealthChecker databaseHealthChecker = new DatabaseHealthChecker(connectionFactory);

        TransactionManager transactionManager = new JdbcTransactionManager(connectionFactory);

        UserRepository userRepository = new JdbcUserRepository();
        UserKeyRepository userKeyRepository = new JdbcUserKeyRepository();
        InboundSecureMessageRepository inboundSecureMessageRepository =
                new JdbcInboundSecureMessageRepository();

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

        JsonUtils jsonUtils = new JsonUtils();

        HealthHandler healthHandler = new HealthHandler();
        UserHandler userHandler = new UserHandler(jsonUtils, registerUserUseCase);
        UserKeyHandler userKeyHandler = new UserKeyHandler(jsonUtils, registerUserKeyUseCase);
        SecureMessageWebhookHandler secureMessageWebhookHandler = new SecureMessageWebhookHandler(jsonUtils, acceptSecureMessageUseCase);

        HttpServerFactory httpServerFactory = new HttpServerFactory(
                appConfig,
                healthHandler,
                userHandler,
                userKeyHandler,
                secureMessageWebhookHandler
        );

        HttpServer httpServer = httpServerFactory.create();

        return new Application(httpServer, databaseHealthChecker);
    }
}