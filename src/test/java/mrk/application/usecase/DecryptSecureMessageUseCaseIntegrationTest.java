package mrk.application.usecase;

import mrk.application.command.DecryptSecureMessageCommand;
import mrk.application.crypto.EncryptedPayload;
import mrk.application.exception.ValidationException;
import mrk.application.port.HybridEncryptionService;
import mrk.application.port.SecureMessageRepository;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserKeyRepository;
import mrk.application.result.DecryptedMessageResult;
import mrk.application.service.DecryptPermissionPolicy;
import mrk.application.service.SecureMessageAuditFactory;
import mrk.application.service.SecureMessageOutboxFactory;
import mrk.config.AppConfig;
import mrk.domain.model.SecureMessage;
import mrk.domain.model.UserKey;
import mrk.infrastructure.crypto.JcaHybridEncryptionService;
import mrk.infrastructure.crypto.JcaKeyManagementService;
import mrk.infrastructure.crypto.RsaPemKeyParser;
import mrk.infrastructure.jdbc.ConnectionFactory;
import mrk.infrastructure.jdbc.JdbcAuditEventRepository;
import mrk.infrastructure.jdbc.JdbcOutboxEventRepository;
import mrk.infrastructure.jdbc.JdbcSecureMessageRepository;
import mrk.infrastructure.jdbc.JdbcTransactionManager;
import mrk.infrastructure.jdbc.JdbcUserKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Testcontainers
class DecryptSecureMessageUseCaseIntegrationTest {

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
    private UserKeyRepository userKeyRepository;
    private SecureMessageRepository secureMessageRepository;
    private HybridEncryptionService encryptionService;

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
        userKeyRepository = new JdbcUserKeyRepository();
        secureMessageRepository = new JdbcSecureMessageRepository();
        encryptionService = new JcaHybridEncryptionService();

        cleanDatabase();
    }

    @Test
    void shouldDecryptMessageWithOldKeyAfterRotation() throws Exception {
        KeyPair oldKeyPair = generateRsaKeyPair();
        KeyPair newKeyPair = generateRsaKeyPair();

        long senderId = insertUser("sender");
        long recipientId = insertUser("recipient");

        long oldKeyId = insertKey(recipientId, oldKeyPair);
        long messageId = insertEncryptedMessage(
                senderId,
                recipientId,
                oldKeyId,
                oldKeyPair.getPublic(),
                "message before rotation",
                false
        );

        rotateKey(recipientId, newKeyPair);

        DecryptSecureMessageUseCase useCase = createUseCase(encryptionService);

        DecryptedMessageResult result = useCase.decrypt(
                new DecryptSecureMessageCommand(messageId, recipientId)
        );

        assertEquals("message before rotation", result.getText());
        assertEquals("READ", loadMessageStatus(messageId));
        assertEquals("REVOKED", loadKeyStatus(oldKeyId));
    }

    @Test
    void shouldAllowOnlyOneConcurrentReadForOneTimeMessage() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();

        long senderId = insertUser("sender");
        long recipientId = insertUser("recipient");
        long keyId = insertKey(recipientId, keyPair);

        long messageId = insertEncryptedMessage(
                senderId,
                recipientId,
                keyId,
                keyPair.getPublic(),
                "one time secret",
                true
        );

        BarrierDecryptService barrierService = new BarrierDecryptService(
                new JcaHybridEncryptionService()
        );

        DecryptSecureMessageUseCase useCase = createUseCase(barrierService);
        DecryptSecureMessageCommand command = new DecryptSecureMessageCommand(messageId, recipientId);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<DecryptedMessageResult> first = executor.submit(() -> useCase.decrypt(command));
            Future<DecryptedMessageResult> second = executor.submit(() -> useCase.decrypt(command));

            int successfulReads = 0;
            int deniedReads = 0;

            for (Future<DecryptedMessageResult> future : List.of(first, second)) {
                try {
                    DecryptedMessageResult result = future.get(10, TimeUnit.SECONDS);
                    assertEquals("one time secret", result.getText());
                    successfulReads++;
                } catch (ExecutionException exception) {
                    assertInstanceOf(ValidationException.class, exception.getCause());
                    deniedReads++;
                }
            }

            assertEquals(1, successfulReads);
            assertEquals(1, deniedReads);
            assertEquals("DESTROYED", loadMessageStatus(messageId));
            assertEquals(1, countAuditEvents("DECRYPT_DENIED", messageId));
        } finally {
            executor.shutdownNow();
        }
    }

    private DecryptSecureMessageUseCase createUseCase(HybridEncryptionService hybridEncryptionService) {
        return new DecryptSecureMessageUseCase(
                transactionManager,
                secureMessageRepository,
                userKeyRepository,
                new JcaKeyManagementService(new RsaPemKeyParser()),
                hybridEncryptionService,
                new JdbcAuditEventRepository(),
                new JdbcOutboxEventRepository(),
                new DecryptPermissionPolicy(),
                new SecureMessageAuditFactory(),
                new SecureMessageOutboxFactory()
        );
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

    private long insertKey(long userId, KeyPair keyPair) {
        return transactionManager.execute(connection -> {
            UserKey key = UserKey.register(
                    userId,
                    toPublicKeyPem(keyPair.getPublic()),
                    toPrivateKeyPem(keyPair.getPrivate()),
                    "key-" + UUID.randomUUID(),
                    "RSA-OAEP-SHA256",
                    LocalDateTime.now()
            );

            return userKeyRepository.save(connection, key);
        });
    }

    private void rotateKey(long userId, KeyPair newKeyPair) {
        transactionManager.execute(connection -> {
            userKeyRepository.revokeActiveKeysByUserId(connection, userId);

            UserKey newKey = UserKey.register(
                    userId,
                    toPublicKeyPem(newKeyPair.getPublic()),
                    toPrivateKeyPem(newKeyPair.getPrivate()),
                    "key-" + UUID.randomUUID(),
                    "RSA-OAEP-SHA256",
                    LocalDateTime.now()
            );

            userKeyRepository.save(connection, newKey);

            return null;
        });
    }

    private long insertEncryptedMessage(
            long senderId,
            long recipientId,
            long keyId,
            PublicKey publicKey,
            String plaintext,
            boolean oneTime
    ) {
        EncryptedPayload encryptedPayload = encryptionService.encrypt(plaintext, publicKey);

        return transactionManager.execute(connection -> {
            long inboundMessageId = insertInboundMessage(
                    connection,
                    senderId,
                    recipientId
            );

            SecureMessage message = SecureMessage.encrypted(
                    inboundMessageId,
                    senderId,
                    recipientId,
                    keyId,
                    encryptedPayload.getEncryptedPayload(),
                    encryptedPayload.getEncryptedContentKey(),
                    encryptedPayload.getNonce(),
                    encryptedPayload.getAlgorithm(),
                    oneTime,
                    LocalDateTime.now().plusHours(1)
            );

            return secureMessageRepository.save(connection, message);
        });
    }

    private long insertInboundMessage(Connection connection, long senderId, long recipientId) throws SQLException {
        String sql = """
                INSERT INTO inbound_secure_messages (
                    request_id,
                    sender_id,
                    recipient_id,
                    plaintext_payload,
                    ttl_seconds,
                    one_time,
                    status,
                    processed_at
                )
                VALUES (?, ?, ?, 'test', 3600, false, 'PROCESSED', CURRENT_TIMESTAMP)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setLong(2, senderId);
            statement.setLong(3, recipientId);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private String loadMessageStatus(long messageId) {
        return queryString(
                "SELECT status FROM secure_messages WHERE id = ?",
                messageId
        );
    }

    private String loadKeyStatus(long keyId) {
        return queryString(
                "SELECT status FROM user_keys WHERE id = ?",
                keyId
        );
    }

    private int countAuditEvents(String eventType, long messageId) {
        return transactionManager.execute(connection -> {
            String sql = """
                    SELECT COUNT(*)
                    FROM audit_events
                    WHERE event_type = ?
                      AND aggregate_id = ?
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, eventType);
                statement.setLong(2, messageId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        });
    }

    private String queryString(String sql, long id) {
        return transactionManager.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getString(1);
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

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String toPublicKeyPem(PublicKey publicKey) {
        String encoded = Base64.getMimeEncoder(
                64,
                "\n".getBytes(StandardCharsets.UTF_8)
        ).encodeToString(publicKey.getEncoded());

        return """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """.formatted(encoded);
    }

    private String toPrivateKeyPem(PrivateKey privateKey) {
        String encoded = Base64.getMimeEncoder(
                64,
                "\n".getBytes(StandardCharsets.UTF_8)
        ).encodeToString(privateKey.getEncoded());

        return """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(encoded);
    }

    private static class BarrierDecryptService implements HybridEncryptionService {

        private final HybridEncryptionService delegate;
        private final CyclicBarrier barrier = new CyclicBarrier(2);

        private BarrierDecryptService(HybridEncryptionService delegate) {
            this.delegate = delegate;
        }

        @Override
        public EncryptedPayload encrypt(String plaintext, PublicKey recipientPublicKey) {
            return delegate.encrypt(plaintext, recipientPublicKey);
        }

        @Override
        public String decrypt(EncryptedPayload encryptedPayload, PrivateKey recipientPrivateKey) {
            String plaintext = delegate.decrypt(encryptedPayload, recipientPrivateKey);

            try {
                // Оба запроса должны закончить decrypt до финальной транзакции.
                barrier.await(5, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new RuntimeException("Concurrent decrypt test failed", exception);
            }

            return plaintext;
        }
    }
}