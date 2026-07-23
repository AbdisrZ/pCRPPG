package id.asr.rppgvitals.infrastructure.persistence.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;
import id.asr.rppgvitals.domain.exception.PersistenceException;
import id.asr.rppgvitals.domain.session.MeasurementRepository;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.session.SessionStatus;
import id.asr.rppgvitals.domain.session.SessionSummary;

/// SQLite adapter implementing the domain [MeasurementRepository] port (`03_ARCHITECTURE.md §6.3`,
/// `10_DATABASE.md`).
///
/// It uses a single injected, long-lived JDBC connection (`10 §7`) that the composition root owns and
/// closes; this class never closes it. On construction it enables WAL mode and foreign-key
/// enforcement and runs [SchemaMigrationRunner]. Every `SQLException` is translated into a
/// [PersistenceException] at this boundary (`00_MASTER_PROMPT.md §22.1`), so no JDBC type escapes
/// inward.
///
/// **Persistence fidelity.** The schema (`10 §3`) stores what the history and trend views need, not
/// the full in-memory object graph: a session's `device_identifier` (not a capture configuration) and
/// per-sample `bpm`/`confidence` (not the producing algorithm, since V1 uses one configured
/// estimator — `08_ESTIMATOR_ENGINE.md §6`). Reconstructed estimates therefore carry a placeholder
/// algorithm identifier. See ADR 0005.
///
/// **Thread-safety.** Not thread-safe; it shares one connection and is driven by the application's
/// single persistence path.
public final class SqliteMeasurementRepository implements MeasurementRepository {

    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_INTERRUPTED = "interrupted";
    private static final String STABLE_QUALITY = "STABLE";
    private static final String RECONSTRUCTED_ALGORITHM = "persisted";
    private static final String DELETE_SAMPLES_SQL = "DELETE FROM heart_rate_samples WHERE session_id = ?";
    private static final String DELETE_SESSION_SQL = "DELETE FROM sessions WHERE id = ?";

    private final Connection connection;
    private final String appVersion;

    /// Wraps a connection, configures the database, and applies pending migrations.
    ///
    /// @param connection an open connection the caller owns and will close; never `null`
    /// @param appVersion the application version to record on each session (`10 §3.1`); never `null`
    public SqliteMeasurementRepository(Connection connection, String appVersion) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.appVersion = Objects.requireNonNull(appVersion, "appVersion");
        configure(connection);
        new SchemaMigrationRunner().migrate(connection);
    }

    private static void configure(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
        } catch (SQLException cause) {
            throw new PersistenceException("configure", "failed to configure the database connection", cause);
        }
    }

    /// {@inheritDoc}
    @Override
    public void save(MeasurementSession session) {
        Objects.requireNonNull(session, "session");
        inTransaction("save", () -> {
            insertSession(session);
            insertSamples(session);
        });
    }

    private void insertSession(MeasurementSession session) throws SQLException {
        List<HeartRateEstimate> estimates = session.estimates();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sessions (id, started_at, ended_at, device_identifier, mean_heart_rate_bpm,"
                        + " mean_confidence, status, app_version) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, session.id().toString());
            statement.setLong(2, session.startedAt().toEpochMilli());
            setNullableLong(
                    statement, 3, session.endedAt().map(Instant::toEpochMilli).orElse(null));
            statement.setString(4, session.deviceIdentifier());
            setNullableDouble(statement, 5, mean(estimates, HeartRateEstimate::beatsPerMinute));
            setNullableDouble(statement, 6, mean(estimates, HeartRateEstimate::confidence));
            statement.setString(7, statusText(session.status()));
            statement.setString(8, appVersion);
            statement.executeUpdate();
        }
    }

    private void insertSamples(MeasurementSession session) throws SQLException {
        long startMillis = session.startedAt().toEpochMilli();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO heart_rate_samples (session_id, offset_seconds, bpm, confidence, signal_quality)"
                        + " VALUES (?, ?, ?, ?, ?)")) {
            for (HeartRateEstimate estimate : session.estimates()) {
                statement.setString(1, session.id().toString());
                statement.setDouble(2, (estimate.timestamp().toEpochMilli() - startMillis) / 1000.0);
                statement.setDouble(3, estimate.beatsPerMinute());
                statement.setDouble(4, estimate.confidence());
                statement.setString(5, STABLE_QUALITY);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /// {@inheritDoc}
    @Override
    public List<SessionSummary> findAllSummaries() {
        try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, started_at, ended_at, status, mean_heart_rate_bpm, mean_confidence"
                                + " FROM sessions ORDER BY started_at DESC");
                ResultSet resultSet = statement.executeQuery()) {
            List<SessionSummary> summaries = new ArrayList<>();
            while (resultSet.next()) {
                summaries.add(mapSummary(resultSet));
            }
            return List.copyOf(summaries);
        } catch (SQLException cause) {
            throw new PersistenceException("findAllSummaries", "failed to list session summaries", cause);
        }
    }

    private static SessionSummary mapSummary(ResultSet resultSet) throws SQLException {
        return new SessionSummary(
                UUID.fromString(resultSet.getString("id")),
                Instant.ofEpochMilli(resultSet.getLong("started_at")),
                nullableInstant(resultSet, "ended_at"),
                fromStatusText(resultSet.getString("status")),
                nullableDouble(resultSet, "mean_heart_rate_bpm"),
                nullableDouble(resultSet, "mean_confidence"));
    }

    /// {@inheritDoc}
    @Override
    public Optional<MeasurementSession> findById(UUID id) {
        Objects.requireNonNull(id, "id");
        try {
            Optional<MeasurementSession> session = loadSession(id);
            session.ifPresent(this::loadEstimatesInto);
            session.ifPresent(loaded -> applyTerminalStatus(loaded, id));
            return session;
        } catch (SQLException cause) {
            throw new PersistenceException("findById", "failed to read session " + id, cause);
        }
    }

    private Optional<MeasurementSession> loadSession(UUID id) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT device_identifier, started_at FROM sessions WHERE id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String deviceIdentifier = resultSet.getString("device_identifier");
                Instant startedAt = Instant.ofEpochMilli(resultSet.getLong("started_at"));
                return Optional.of(new MeasurementSession(id, deviceIdentifier, startedAt));
            }
        }
    }

    private void loadEstimatesInto(MeasurementSession session) {
        long startMillis = session.startedAt().toEpochMilli();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT offset_seconds, bpm, confidence FROM heart_rate_samples WHERE session_id = ?"
                        + " AND bpm IS NOT NULL ORDER BY offset_seconds")) {
            statement.setString(1, session.id().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Instant timestamp = Instant.ofEpochMilli(
                            startMillis + Math.round(resultSet.getDouble("offset_seconds") * 1000.0));
                    session.recordEstimate(new HeartRateEstimate(
                            resultSet.getDouble("bpm"),
                            resultSet.getDouble("confidence"),
                            timestamp,
                            RECONSTRUCTED_ALGORITHM));
                }
            }
        } catch (SQLException cause) {
            throw new PersistenceException("findById", "failed to read samples for session " + session.id(), cause);
        }
    }

    private void applyTerminalStatus(MeasurementSession session, UUID id) {
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT ended_at, status FROM sessions WHERE id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                Instant endedAt = Instant.ofEpochMilli(resultSet.getLong("ended_at"));
                if (STATUS_COMPLETED.equals(resultSet.getString("status"))) {
                    session.complete(endedAt);
                } else {
                    session.abort(endedAt);
                }
            }
        } catch (SQLException cause) {
            throw new PersistenceException("findById", "failed to read status for session " + id, cause);
        }
    }

    /// {@inheritDoc}
    @Override
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "id");
        inTransaction("deleteById", () -> {
            deleteSamplesOf(id);
            deleteSessionRow(id);
        });
    }

    private void deleteSamplesOf(UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SAMPLES_SQL)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        }
    }

    private void deleteSessionRow(UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SESSION_SQL)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        }
    }

    private void inTransaction(String operation, SqlWork work) {
        try {
            connection.setAutoCommit(false);
            try {
                work.run();
                connection.commit();
            } catch (SQLException cause) {
                connection.rollback();
                throw cause;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException cause) {
            throw new PersistenceException(operation, "transaction failed during " + operation, cause);
        }
    }

    private static String statusText(SessionStatus status) {
        return switch (status) {
            case COMPLETED -> STATUS_COMPLETED;
            case ABORTED -> STATUS_INTERRUPTED;
            case ACTIVE -> throw new IllegalStateException("an active session cannot be persisted");
        };
    }

    private static SessionStatus fromStatusText(String text) {
        return STATUS_COMPLETED.equals(text) ? SessionStatus.COMPLETED : SessionStatus.ABORTED;
    }

    private static Double mean(
            List<HeartRateEstimate> estimates, java.util.function.ToDoubleFunction<HeartRateEstimate> field) {
        return estimates.isEmpty()
                ? null
                : estimates.stream().mapToDouble(field).average().getAsDouble();
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.REAL);
        } else {
            statement.setDouble(index, value);
        }
    }

    private static Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : Instant.ofEpochMilli(value);
    }

    @FunctionalInterface
    private interface SqlWork {
        void run() throws SQLException;
    }
}
