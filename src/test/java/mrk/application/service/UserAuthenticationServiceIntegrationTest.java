package mrk.application.service;

import mrk.application.command.RegisterUserCommand;
import mrk.application.exception.AuthenticationException;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserRepository;
import mrk.application.result.RegisterUserResult;
import mrk.application.usecase.RegisterUserUseCase;
import mrk.config.AppConfig;
import mrk.infrastructure.jdbc.ConnectionFactory;
import mrk.infrastructure.jdbc.JdbcTransactionManager;
import mrk.infrastructure.jdbc.JdbcUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class UserAuthenticationServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("threadbridge_test")
                    .withUsername("threadbridge_test")
                    .withPassword("threadbridge_test")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "docker/postgres/init/001_init_schema.sql"
                            ),
                            "/docker-entrypoint-initdb.d/001_init_schema.sql"
                    );

    private TransactionManager transactionManager;
    private ApiTokenService apiTokenService;
    private RegisterUserUseCase registerUserUseCase;
    private UserAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        ConnectionFactory connectionFactory =
                new ConnectionFactory(new AppConfig()) {

                    @Override
                    public Connection createConnection()
                            throws SQLException {

                        return DriverManager.getConnection(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword()
                        );
                    }
                };

        transactionManager =
                new JdbcTransactionManager(connectionFactory);

        UserRepository userRepository =
                new JdbcUserRepository();

        apiTokenService =
                new ApiTokenService();

        registerUserUseCase =
                new RegisterUserUseCase(
                        transactionManager,
                        userRepository,
                        apiTokenService
                );

        authenticationService =
                new UserAuthenticationService(
                        transactionManager,
                        userRepository,
                        apiTokenService
                );

        cleanDatabase();
    }

    @Test
    void shouldAuthenticateRegisteredUserByApiToken() {
        RegisterUserResult result =
                registerUserUseCase.register(
                        new RegisterUserCommand(
                                "user-" + UUID.randomUUID()
                        )
                );

        long authenticatedUserId =
                authenticationService.authenticate(
                        result.getApiToken()
                );

        assertEquals(
                result.getUserId(),
                authenticatedUserId
        );
    }

    @Test
    void shouldRejectInvalidToken() {
        registerUserUseCase.register(
                new RegisterUserCommand(
                        "user-" + UUID.randomUUID()
                )
        );

        assertThrows(
                AuthenticationException.class,
                () -> authenticationService.authenticate(
                        "invalid-token"
                )
        );
    }

    @Test
    void shouldStoreOnlyTokenHash() {
        RegisterUserResult result =
                registerUserUseCase.register(
                        new RegisterUserCommand(
                                "user-" + UUID.randomUUID()
                        )
                );

        String storedHash =
                loadStoredTokenHash(result.getUserId());

        assertNotEquals(
                result.getApiToken(),
                storedHash
        );

        assertEquals(
                apiTokenService.hash(result.getApiToken()),
                storedHash
        );
    }

    private String loadStoredTokenHash(long userId) {
        return transactionManager.execute(connection -> {
            String sql = """
                    SELECT api_token_hash
                    FROM users
                    WHERE id = ?
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setLong(1, userId);

                try (ResultSet resultSet =
                             statement.executeQuery()) {

                    resultSet.next();

                    return resultSet.getString(
                            "api_token_hash"
                    );
                }
            }
        });
    }

    private void cleanDatabase() {
        transactionManager.execute(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("""
                                 TRUNCATE TABLE
                                     notifications,
                                     audit_events,
                                     outbox_events,
                                     secure_messages,
                                     inbound_secure_messages,
                                     user_keys,
                                     users
                                 RESTART IDENTITY CASCADE
                                 """)) {

                statement.executeUpdate();
            }

            return null;
        });
    }
}