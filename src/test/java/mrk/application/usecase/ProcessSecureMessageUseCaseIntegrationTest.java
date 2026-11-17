package mrk.application.usecase;

import mrk.application.command.ProcessSecureMessageCommand;
import mrk.application.command.RegisterUserKeyCommand;
import mrk.application.crypto.EncryptedPayload;
import mrk.application.port.*;
import mrk.application.result.ProcessSecureMessageResult;
import mrk.application.result.RegisterUserKeyResult;
import mrk.config.AppConfig;
import mrk.infrastructure.crypto.JcaHybridEncryptionService;
import mrk.infrastructure.crypto.JcaKeyManagementService;
import mrk.infrastructure.crypto.RsaPemKeyParser;
import mrk.infrastructure.jdbc.*;
import mrk.utils.KeyFingerprintCalculator;
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
import java.sql.*;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ProcessSecureMessageUseCaseIntegrationTest {

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
    private UserRepository userRepository;
    private UserKeyRepository userKeyRepository;
    private InboundSecureMessageRepository inboundRepository;
    private SecureMessageRepository secureMessageRepository;
    private RegisterUserKeyUseCase registerUserKeyUseCase;

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
        userRepository = new JdbcUserRepository();
        userKeyRepository = new JdbcUserKeyRepository();
        inboundRepository = new JdbcInboundSecureMessageRepository();
        secureMessageRepository = new JdbcSecureMessageRepository();

        registerUserKeyUseCase = new RegisterUserKeyUseCase(
                transactionManager,
                userRepository,
                userKeyRepository,
                new KeyFingerprintCalculator()
        );

        cleanDatabase();
    }

    @Test
    void shouldRepeatEncryptionWhenRecipientKeyWasRotated() throws Exception {
        KeyPair firstKeyPair = generateRsaKeyPair();
        KeyPair secondKeyPair = generateRsaKeyPair();

        long senderId = insertUser("sender-" + UUID.randomUUID());
        long recipientId = insertUser("recipient-" + UUID.randomUUID());

        RegisterUserKeyResult firstKey = registerKey(recipientId, firstKeyPair);

        long inboundMessageId = insertProcessingInboundMessage(
                senderId,
                recipientId,
                "secret message"
        );

        RotationEncryptionService encryptionService = new RotationEncryptionService(
                new JcaHybridEncryptionService(),
                () -> registerKey(recipientId, secondKeyPair)
        );

        ProcessSecureMessageUseCase useCase = createUseCase(encryptionService);

        ProcessSecureMessageResult result = useCase.process(
                new ProcessSecureMessageCommand(inboundMessageId)
        );

        assertEquals(2, encryptionService.getEncryptCount());
        assertNotEquals(firstKey.getKeyId(), loadRecipientKeyId(result.getSecureMessageId()));
        assertEquals("REVOKED", loadKeyStatus(firstKey.getKeyId()));
        assertEquals("PROCESSED", loadInboundStatus(inboundMessageId));
        assertNull(loadInboundPlaintext(inboundMessageId));
    }

    private ProcessSecureMessageUseCase createUseCase(HybridEncryptionService encryptionService) {
        KeyManagementService keyManagementService = new JcaKeyManagementService(
                new RsaPemKeyParser()
        );

        return new ProcessSecureMessageUseCase(
                transactionManager,
                inboundRepository,
                secureMessageRepository,
                new JdbcOutboxEventRepository(),
                userRepository,
                userKeyRepository,
                keyManagementService,
                encryptionService,
                new JdbcAuditEventRepository()
        );
    }

    private RegisterUserKeyResult registerKey(long userId, KeyPair keyPair) {
        return registerUserKeyUseCase.register(
                new RegisterUserKeyCommand(
                        userId,
                        toPublicKeyPem(keyPair.getPublic()),
                        toPrivateKeyPem(keyPair.getPrivate()),
                        "RSA-OAEP-SHA256"
                )
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
                statement.setString(1, username);

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getLong("id");
                }
            }
        });
    }

    private long insertProcessingInboundMessage(long senderId, long recipientId, String plaintext) {
        return transactionManager.execute(connection -> {
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
                    VALUES (?, ?, ?, ?, 3600, false, 'PROCESSING', CURRENT_TIMESTAMP)
                    RETURNING id
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, UUID.randomUUID().toString());
                statement.setLong(2, senderId);
                statement.setLong(3, recipientId);
                statement.setString(4, plaintext);

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getLong("id");
                }
            }
        });
    }

    private long loadRecipientKeyId(long secureMessageId) {
        return queryLong(
                "SELECT recipient_key_id FROM secure_messages WHERE id = ?",
                secureMessageId
        );
    }

    private String loadKeyStatus(long keyId) {
        return queryString(
                "SELECT status FROM user_keys WHERE id = ?",
                keyId
        );
    }

    private String loadInboundStatus(long inboundMessageId) {
        return queryString(
                "SELECT status FROM inbound_secure_messages WHERE id = ?",
                inboundMessageId
        );
    }

    private String loadInboundPlaintext(long inboundMessageId) {
        return queryString(
                "SELECT plaintext_payload FROM inbound_secure_messages WHERE id = ?",
                inboundMessageId
        );
    }

    private long queryLong(String sql, long id) {
        return transactionManager.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getLong(1);
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
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        DELETE FROM outbox_events;
                        DELETE FROM audit_events;
                        DELETE FROM notifications;
                        DELETE FROM secure_messages;
                        DELETE FROM inbound_secure_messages;
                        DELETE FROM user_keys;
                        DELETE FROM users;
                        """);
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

    private static class RotationEncryptionService implements HybridEncryptionService {

        private final HybridEncryptionService delegate;
        private final Runnable rotation;
        private int encryptCount;

        private RotationEncryptionService(HybridEncryptionService delegate, Runnable rotation) {
            this.delegate = delegate;
            this.rotation = rotation;
        }

        @Override
        public EncryptedPayload encrypt(String plaintext, PublicKey recipientPublicKey) {
            EncryptedPayload result = delegate.encrypt(plaintext, recipientPublicKey);

            encryptCount++;

            if (encryptCount == 1) {
                rotation.run();
            }

            return result;
        }

        @Override
        public String decrypt(EncryptedPayload encryptedPayload, PrivateKey recipientPrivateKey) {
            return delegate.decrypt(encryptedPayload, recipientPrivateKey);
        }

        private int getEncryptCount() {
            return encryptCount;
        }
    }
}