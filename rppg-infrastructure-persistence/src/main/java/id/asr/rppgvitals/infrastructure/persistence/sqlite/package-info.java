/// SQLite adapter implementing the domain `MeasurementRepository` port.
///
/// Single-responsibility infrastructure package (`04 §3`): `SqliteMeasurementRepository`
/// persists and retrieves `MeasurementSession` records against a local SQLite database via the
/// JDBC driver, executing the schema DDL at startup. JDBC `SQLException`s are translated into
/// `PersistenceException` at this boundary (`00 §22.1`); no `org.sqlite.*` type escapes inward.
/// Governed by `03_ARCHITECTURE.md §6.3` and `10_DATABASE.md §9`.
package id.asr.rppgvitals.infrastructure.persistence.sqlite;
