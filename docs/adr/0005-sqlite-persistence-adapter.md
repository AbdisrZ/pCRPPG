# ADR 0005 — SQLite Persistence Adapter and Schema-Driven Domain Reconciliation

- **Status:** Accepted
- **Date:** 2026-07-23
- **Decision process:** `00_MASTER_PROMPT.md §8`, node G. The domain changes touch already-"Done" T-101
  types; they are recorded here per `00 §20.2` (modifying existing code) and `00 §8`.
- **Implements:** T-204 (schema DDL + migration runner), T-205 (`SqliteMeasurementRepository`), in
  `rppg-infrastructure-persistence` (`10_DATABASE.md`).

## Context

`10_DATABASE.md` is the binding schema; `03 §3` had explicitly deferred domain field types to
implementation. Building the adapter revealed that two T-101 guesses did not match the binding
schema, so the domain was reconciled to the schema (the authority), not the reverse. Validated by a
green `./mvnw -P ci clean verify` including 10 integration tests against a real temporary SQLite
database (round-trip, ordering, delete-with-cascade, absent, aborted, active-rejection,
closed-connection error, migration idempotency).

## Decisions

1. **`MeasurementSession.device`: `CaptureConfiguration` → `String deviceIdentifier`.** The `sessions`
   table stores only `device_identifier` (`10 §3.1`); a session records *which device was used*, not
   the full capture configuration (that lives at capture time). This makes the round-trip faithful
   instead of fabricating a placeholder `CaptureConfiguration` on read.
2. **`SessionSummary`: `estimateCount` + `averageBeatsPerMinute` → nullable `meanHeartRateBpm` +
   `meanConfidence`.** The summary now mirrors one `sessions` row, so `findAllSummaries` is a cheap
   select with no join to `heart_rate_samples` (`10 §9`). The means are nullable (a completed session
   that produced no estimate has none, `10 §3.1`) — a documented nullable-boxed field at the
   persistence boundary, the exception `05 §7` permits.
3. **Status mapping.** Domain `COMPLETED → "completed"`, `ABORTED → "interrupted"` (`10 §3.1`); an
   `ACTIVE` session cannot be persisted (rejected with `IllegalStateException`), matching the
   buffer-then-commit-on-end design of `10 §6`.
4. **Persistence-fidelity limits (documented, not defects).** The schema deliberately stores less
   than the in-memory graph: (a) the producing **algorithm** is not stored (V1 uses one configured
   estimator, `08 §6`), so reconstructed estimates carry a placeholder `"persisted"` algorithm; (b)
   only the device identifier, not resolution/fps; (c) the entity writes one `STABLE` sample per
   `HeartRateEstimate` — the schema's null-`bpm` `SEARCHING`/`DEGRADED` trend rows are an orchestrator
   concern (T-302), not produced here.
5. **Migration runner (`10 §5`).** Hand-rolled, no framework (`00 §16`): an ordered list of numbered
   `schema/NNN_*.sql` resources, each applied in its own transaction, `schema_version` bumped after.
   Comments are stripped from the whole script *before* splitting on `;` (a `;` inside a comment must
   not split a statement — this was a real bug caught by the integration tests).
6. **Connection & transactions (`10 §6`, `§7`).** One injected, long-lived connection the composition
   root owns and closes — the repository never closes it. WAL and `foreign_keys` pragmas are set on
   construction. `save` and `deleteById` each run in a single transaction; samples are batch-inserted;
   delete removes samples then the session row explicitly (not relying on cascade alone).
7. **`sqlite-jdbc` 3.53.2.1** added to the approved set (already listed in `00 §16`), pinned exactly.
   It bundles native binaries for all platforms in one jar, so it needs no `native.classifier` from
   the T-004 profiles.
8. **One recorded SpotBugs suppression** (`config/spotbugs/exclude.xml`): executing DDL read from a
   bundled resource is inherently "non-constant SQL" but is not an injection surface (trusted, shipped
   in the jar). Scoped narrowly to `SchemaMigrationRunner.applyMigration`. The repository's own deletes
   use constant SQL with parameterised ids — no suppression needed there.

## Consequences

- The persistence adapter round-trips a `MeasurementSession` faithfully within the schema's columns,
  with the fidelity limits above documented on the class and here.
- Two T-101 domain types changed shape; all domain tests were updated and pass. Callers elsewhere are
  none yet (the application/use-case layer is Phase 3).
- Phase 2 remaining: T-201 `OpenCvFrameSource` (native OpenCV) and T-202/T-203 ONNX (blocked on
  NC-001).
