package mrk.application.usecase;

import mrk.application.command.AcceptSecureMessageCommand;
import mrk.application.port.InboundSecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.result.AcceptSecureMessageResult;
import mrk.config.AppConfig;
import mrk.domain.model.InboundSecureMessage;
import mrk.infrastructure.jdbc.ConnectionFactory;
import mrk.infrastructure.jdbc.JdbcAuditEventRepository;
import mrk.infrastructure.jdbc.JdbcInboundSecureMessageRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class AcceptSecureMessageUseCaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("threadbridge_test")
            .withUsername("threadbridge_test")
            .withPassword("threadbridge_test")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("docker/postgres/init/001_init_schema.sql"),
                    "/docker-entrypoint-initdb.d/001_init_schema.sql"
            );

    private TransactionManager transactionManager;
    private AcceptSecureMessageUseCase useCase;

    @BeforeEach
    void setUp() {
        ConnectionFactory connectionFactory = new ConnectionFactory(new AppConfig()) {
            @Override
            public Connection createConnection() throws SQLException {
                return DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword()
                );
            }
        };

        transactionManager = new JdbcTransactionManager(connectionFactory);

        InboundSecureMessageRepository inboundRepository = new BarrierInboundSecureMessageRepository();

        useCase = new AcceptSecureMessageUseCase(
                transactionManager,
                new JdbcUserRepository(),
                inboundRepository,
                new JdbcAuditEventRepository(),
                10_000,
                60,
                86_400
        );

        cleanDatabase();
    }

    @Test
    void shouldAcceptConcurrentRequestsIdempotently() throws Exception {
        long senderId = insertUser("sender");
        long recipientId = insertUser("recipient");
        String requestId = "request-" + UUID.randomUUID();

        AcceptSecureMessageCommand command = new AcceptSecureMessageCommand(
                requestId,
                senderId,
                recipientId,
                "secret message",
                3600,
                false
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<AcceptSecureMessageResult> firstFuture = executor.submit(() -> useCase.accept(command));
            Future<AcceptSecureMessageResult> secondFuture = executor.submit(() -> useCase.accept(command));

            AcceptSecureMessageResult firstResult = firstFuture.get(10, TimeUnit.SECONDS);
            AcceptSecureMessageResult secondResult = secondFuture.get(10, TimeUnit.SECONDS);

            assertEquals(firstResult.getInboundMessageId(), secondResult.getInboundMessageId());

            long duplicateCount = List.of(firstResult, secondResult)
                    .stream()
                    .filter(AcceptSecureMessageResult::isDuplicate)
                    .count();

            assertEquals(1, duplicateCount);
            assertEquals(1, countInboundMessages(requestId));
            assertEquals(1, countAcceptedAuditEvents(firstResult.getInboundMessageId()));
        } finally {
            executor.shutdownNow();
        }
    }

    private long insertUser(String username) {
        return transactionManager.execute(connection -> {
            String sql = """
                    INSERT INTO users (username, status)
                    VALUES (?, 'ACTIVE')
                    RETURNING id
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username + "-" + UUID.randomUUID());

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getLong("id");
                }
            }
        });
    }

    private int countInboundMessages(String requestId) {
        return transactionManager.execute(connection -> {
            String sql = """
                    SELECT COUNT(*)
                    FROM inbound_secure_messages
                    WHERE request_id = ?
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, requestId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        });
    }

    private int countAcceptedAuditEvents(long inboundMessageId) {
        return transactionManager.execute(connection -> {
            String sql = """
                    SELECT COUNT(*)
                    FROM audit_events
                    WHERE event_type = 'INBOUND_ACCEPTED'
                      AND aggregate_id = ?
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, inboundMessageId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        });
    }

    private void cleanDatabase() {
        transactionManager.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
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

    private static class BarrierInboundSecureMessageRepository extends JdbcInboundSecureMessageRepository {

        private final CyclicBarrier barrier = new CyclicBarrier(2);

        @Override
        public Optional<Long> saveIfAbsent(Connection connection, InboundSecureMessage message) {
            try {
                // Оба запроса должны дойти до INSERT до продолжения теста.
                barrier.await(5, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new RuntimeException("Concurrent accept test failed", exception);
            }

            return super.saveIfAbsent(connection, message);
        }
    }
}