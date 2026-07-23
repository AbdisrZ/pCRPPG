package id.asr.rppgvitals.infrastructure.persistence.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;
import id.asr.rppgvitals.domain.exception.PersistenceException;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.session.SessionStatus;
import id.asr.rppgvitals.domain.session.SessionSummary;

class SqliteMeasurementRepositoryIntegrationTest {

    private static final Instant START = Instant.parse("2026-07-23T09:00:00Z");
    private static final Instant END = Instant.parse("2026-07-23T09:05:00Z");

    @TempDir
    private Path databaseDirectory;

    private Connection connection;
    private SqliteMeasurementRepository repository;

    @BeforeEach
    void openDatabase() throws SQLException {
        Path databaseFile = databaseDirectory.resolve("vitals.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        repository = new SqliteMeasurementRepository(connection, "test-0.1.0");
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    private static MeasurementSession completedSession(UUID id, Instant startedAt) {
        MeasurementSession session = new MeasurementSession(id, "cam-0", startedAt);
        session.recordEstimate(new HeartRateEstimate(70.0, 0.7, startedAt.plusSeconds(1), "CHROM"));
        session.recordEstimate(new HeartRateEstimate(80.0, 0.9, startedAt.plusSeconds(2), "CHROM"));
        session.complete(startedAt.plusSeconds(180));
        return session;
    }

    @Test
    void save_thenFindById_roundTripsTheSession() {
        UUID id = UUID.randomUUID();

        repository.save(completedSession(id, START));
        Optional<MeasurementSession> loaded = repository.findById(id);

        assertTrue(loaded.isPresent());
        MeasurementSession session = loaded.orElseThrow();
        assertEquals(id, session.id());
        assertEquals("cam-0", session.deviceIdentifier());
        assertEquals(START, session.startedAt());
        assertEquals(START.plusSeconds(180), session.endedAt().orElseThrow());
        assertEquals(SessionStatus.COMPLETED, session.status());
        assertEquals(2, session.estimates().size());
        assertEquals(70.0, session.estimates().get(0).beatsPerMinute());
        assertEquals(80.0, session.estimates().get(1).beatsPerMinute());
    }

    @Test
    void save_abortedSession_roundTripsAsAborted() {
        UUID id = UUID.randomUUID();
        MeasurementSession session = new MeasurementSession(id, "cam-1", START);
        session.abort(END);

        repository.save(session);

        assertEquals(
                SessionStatus.ABORTED, repository.findById(id).orElseThrow().status());
    }

    @Test
    void save_completedSessionWithNoEstimates_storesNullMeans() {
        UUID id = UUID.randomUUID();
        MeasurementSession session = new MeasurementSession(id, "cam-0", START);
        session.complete(END);

        repository.save(session);

        SessionSummary summary = repository.findAllSummaries().get(0);
        assertNull(summary.meanHeartRateBpm());
        assertNull(summary.meanConfidence());
        assertTrue(repository.findById(id).orElseThrow().estimates().isEmpty());
    }

    @Test
    void findAllSummaries_ordersByStartDescendingAndComputesMeans() {
        UUID older = UUID.randomUUID();
        UUID newer = UUID.randomUUID();
        repository.save(completedSession(older, START));
        repository.save(completedSession(newer, START.plusSeconds(600)));

        List<SessionSummary> summaries = repository.findAllSummaries();

        assertEquals(2, summaries.size());
        assertEquals(newer, summaries.get(0).sessionId());
        assertEquals(older, summaries.get(1).sessionId());
        assertEquals(75.0, summaries.get(0).meanHeartRateBpm());
    }

    @Test
    void deleteById_removesSessionAndItsSamples() {
        UUID id = UUID.randomUUID();
        repository.save(completedSession(id, START));

        repository.deleteById(id);

        assertTrue(repository.findById(id).isEmpty());
        assertTrue(repository.findAllSummaries().isEmpty());
    }

    @Test
    void deleteById_absentSession_isANoOp() {
        repository.deleteById(UUID.randomUUID());

        assertTrue(repository.findAllSummaries().isEmpty());
    }

    @Test
    void findById_absentSession_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void save_activeSession_isRejected() {
        MeasurementSession active = new MeasurementSession(UUID.randomUUID(), "cam-0", START);

        assertThrows(IllegalStateException.class, () -> repository.save(active));
    }

    @Test
    void secondInstance_onSameDatabase_appliesNoFurtherMigrations() {
        UUID id = UUID.randomUUID();
        repository.save(completedSession(id, START));

        SqliteMeasurementRepository reopened = new SqliteMeasurementRepository(connection, "test-0.1.0");

        assertFalse(reopened.findAllSummaries().isEmpty());
    }

    @Test
    void operation_onClosedConnection_throwsPersistenceException() throws SQLException {
        connection.close();

        assertThrows(PersistenceException.class, () -> repository.findAllSummaries());
    }
}
