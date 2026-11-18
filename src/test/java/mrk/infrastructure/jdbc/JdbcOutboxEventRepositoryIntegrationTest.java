package mrk.infrastructure.jdbc;

import mrk.domain.model.OutboxEvent;
import mrk.domain.value.OutboxEventStatus;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class JdbcOutboxEventRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("threadbridge_test")
            .withUsername("threadbridge_test")
            .withPassword("threadbridge_test")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("docker/postgres/init/001_init_schema.sql"),
                    "/docker-entrypoint-initdb.d/001_init_schema.sql"
            );

    private JdbcOutboxEventRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        repository = new JdbcOutboxEventRepository();

        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement(
                     "TRUNCATE TABLE outbox_events RESTART IDENTITY"
             )) {

            statement.executeUpdate();
        }
    }

    @Test
    void shouldClaimNewAndStaleProcessingEvents() throws Exception {
        long newEventId = insertEvent("NEW", "NULL");

        long staleEventId = insertEvent(
                "PROCESSING",
                "CURRENT_TIMESTAMP - INTERVAL '2 minutes'"
        );

        long freshEventId = insertEvent(
                "PROCESSING",
                "CURRENT_TIMESTAMP"
        );

        List<OutboxEvent> claimedEvents;

        try (Connection connection = POSTGRES.createConnection("")) {
            connection.setAutoCommit(false);

            claimedEvents = repository.claimProcessableBatch(
                    connection,
                    10,
                    30
            );

            connection.commit();
        }

        Set<Long> claimedIds = claimedEvents.stream()
                .map(OutboxEvent::getId)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of(newEventId, staleEventId),
                claimedIds
        );

        assertTrue(
                claimedEvents.stream()
                        .allMatch(event -> event.getStatus() == OutboxEventStatus.PROCESSING)
        );

        assertEquals("PROCESSING", loadStatus(newEventId));
        assertEquals("PROCESSING", loadStatus(staleEventId));
        assertEquals("PROCESSING", loadStatus(freshEventId));
    }

    private long insertEvent(String status, String processingStartedAt) throws Exception {
        String sql = """
                INSERT INTO outbox_events (
                    event_type,
                    aggregate_type,
                    aggregate_id,
                    payload,
                    status,
                    processing_started_at
                )
                VALUES (
                    'TEST_EVENT',
                    'Test',
                    1,
                    '{}',
                    ?,
                    %s
                )
                RETURNING id
                """.formatted(processingStartedAt);

        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private String loadStatus(long eventId) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT status
                     FROM outbox_events
                     WHERE id = ?
                     """)) {

            statement.setLong(1, eventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString("status");
            }
        }
    }
}