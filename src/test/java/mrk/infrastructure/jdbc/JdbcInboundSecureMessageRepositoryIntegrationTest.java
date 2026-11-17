package mrk.infrastructure.jdbc;

import mrk.domain.model.InboundSecureMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class JdbcInboundSecureMessageRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("threadbridge_test")
            .withUsername("threadbridge_test")
            .withPassword("threadbridge_test")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("docker/postgres/init/001_init_schema.sql"),
                    "/docker-entrypoint-initdb.d/001_init_schema.sql"
            );

    private JdbcInboundSecureMessageRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        repository = new JdbcInboundSecureMessageRepository();

        try (Connection connection = POSTGRES.createConnection("")) {
            connection.createStatement().execute("""
                    DELETE FROM outbox_events;
                    DELETE FROM audit_events;
                    DELETE FROM notifications;
                    DELETE FROM secure_messages;
                    DELETE FROM inbound_secure_messages;
                    DELETE FROM user_keys;
                    DELETE FROM users;
                    """);
        }
    }

    @Test
    void shouldReclaimOnlyStaleProcessingMessages() throws Exception {
        long senderId = insertUser("sender");
        long recipientId = insertUser("recipient");

        long staleMessageId = insertProcessingMessage(
                senderId,
                recipientId,
                "CURRENT_TIMESTAMP - INTERVAL '2 minutes'"
        );

        insertProcessingMessage(
                senderId,
                recipientId,
                "CURRENT_TIMESTAMP"
        );

        List<InboundSecureMessage> claimed;

        try (Connection connection = POSTGRES.createConnection("")) {
            connection.setAutoCommit(false);

            claimed = repository.claimProcessableBatch(
                    connection,
                    10,
                    30
            );

            connection.commit();
        }

        assertEquals(1, claimed.size());
        assertEquals(staleMessageId, claimed.getFirst().getId());
        assertEquals("PROCESSING", loadStatus(staleMessageId));
    }

    private long insertUser(String name) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO users (username, status)
                     VALUES (?, 'ACTIVE')
                     RETURNING id
                     """)) {

            statement.setString(1, name + "-" + UUID.randomUUID());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private long insertProcessingMessage(long senderId, long recipientId, String processingStartedAt) throws Exception {
        String sql = """
                INSERT INTO inbound_secure_messages (
                    request_id,
                    sender_id,
                    recipient_id,
                    plaintext_payload,
                    ttl_seconds,
                    one_time,
                    status,
                    processing_started_at
                )
                VALUES (?, ?, ?, 'message', 3600, false, 'PROCESSING', %s)
                RETURNING id
                """.formatted(processingStartedAt);

        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, UUID.randomUUID().toString());
            statement.setLong(2, senderId);
            statement.setLong(3, recipientId);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private String loadStatus(long messageId) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT status
                     FROM inbound_secure_messages
                     WHERE id = ?
                     """)) {

            statement.setLong(1, messageId);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString("status");
            }
        }
    }
}