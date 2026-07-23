package id.asr.rppgvitals.infrastructure.persistence.sqlite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import id.asr.rppgvitals.domain.exception.PersistenceException;

/// The hand-rolled schema migration runner of `10_DATABASE.md §5`.
///
/// No migration framework is added to the approved dependency set (`00_MASTER_PROMPT.md §16`); at V1
/// scale a single ordered list of numbered DDL scripts (`04_PACKAGE_STRUCTURE.md §6`) is enough. On
/// each run it reads the current `schema_version`, then applies every pending script in order, each
/// inside its own transaction, bumping `schema_version` after the script succeeds. Package-private
/// collaborator of [SqliteMeasurementRepository].
final class SchemaMigrationRunner {

    private static final String SCHEMA_RESOURCE_PREFIX = "/schema/";

    private static final List<Migration> MIGRATIONS = List.of(new Migration(1, "001_initial_schema.sql"));

    private record Migration(int version, String resource) {}

    /// Applies every migration whose version is higher than the currently applied schema version.
    ///
    /// @param connection an open connection to the database to migrate; never `null`
    /// @throws PersistenceException if reading the version or applying a script fails
    void migrate(Connection connection) {
        try {
            ensureSchemaVersionTable(connection);
            int currentVersion = currentVersion(connection);
            for (Migration migration : MIGRATIONS) {
                if (migration.version() > currentVersion) {
                    applyMigration(connection, migration);
                }
            }
        } catch (SQLException cause) {
            throw new PersistenceException("migrate", "failed to apply schema migrations", cause);
        }
    }

    private static void ensureSchemaVersionTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL, applied_at INTEGER NOT NULL)");
        }
    }

    private static int currentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private void applyMigration(Connection connection, Migration migration) throws SQLException {
        List<String> statements = readStatements(migration.resource());
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            for (String sql : statements) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            recordVersion(connection, migration.version());
            connection.commit();
        } catch (SQLException cause) {
            connection.rollback();
            throw cause;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static void recordVersion(Connection connection, int version) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("INSERT INTO schema_version (version, applied_at) VALUES (?, ?)")) {
            statement.setInt(1, version);
            statement.setLong(2, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private List<String> readStatements(String resource) {
        // Comments are stripped from the whole script *before* splitting on ';', so a semicolon that
        // appears inside a comment line cannot split a statement in two.
        String script = stripComments(readResource(SCHEMA_RESOURCE_PREFIX + resource));
        List<String> statements = new ArrayList<>();
        for (String rawStatement : script.split(";")) {
            String sql = rawStatement.trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    private static String stripComments(String script) {
        return Arrays.stream(script.split("\n"))
                .filter(line -> !line.trim().startsWith("--"))
                .reduce("", (a, b) -> a + b + "\n");
    }

    private String readResource(String path) {
        try (InputStream stream = SchemaMigrationRunner.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new PersistenceException("migrate", "schema resource not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException cause) {
            throw new PersistenceException("migrate", "failed to read schema resource: " + path, cause);
        }
    }
}
