# 10_DATABASE.md
# Database Design
## rPPG Desktop Vitals Monitor

---

**Document Control**

| Field | Value |
|---|---|
| Document ID | DB-10 |
| Version | 1.0.0 |
| Status | **BINDING** — Infrastructure Adapter Specification |
| Depends On | `03_ARCHITECTURE.md` (§3, §4, §6.3), `02_SOFTWARE_REQUIREMENT.md` (§5, §3.2) |
| Consumed By | `12_PERFORMANCE.md`, `14_DEPLOYMENT.md` |
| Precedence | Subordinate to `02_SOFTWARE_REQUIREMENT.md §5`. No schema element in this document may store data category (a) from that section — that constraint is structural, not a runtime check to remember to add. |
| Maintainer | Human Project Architect — Abdi Soleh Rosadi |
| Last Updated | 2026-07-12 |

---

## 1. Purpose of This Document

`03_ARCHITECTURE.md` names `MeasurementRepository` and its implementation, `SqliteMeasurementRepository`, without specifying a schema. `02_SOFTWARE_REQUIREMENT.md §5` constrains what that schema is *allowed* to contain before a single column is designed. This document is where those two documents meet: the actual table layout, and the concrete guarantee that the persistence layer cannot silently drift into storing something `02 §5` forbids.

---

## 2. Data Categories and the Persistence Boundary

Restating `02 §5` at the level this schema must enforce: category (a), raw frames, is never persisted — no table in this schema has an image, pixel-buffer, or BLOB-typed column of any kind, anywhere. This is verifiable by inspecting §3 directly: its absence is structural, not a rule someone has to remember to follow at write time. Categories (b) and (c) — derived signal data and session metadata — are exactly what the two tables below hold.

---

## 3. Schema Design

### 3.1 `sessions`

One row per `MeasurementSession` (`03 §3`), corresponding to `SessionSummary` (`02` FR-201).

| Column | Type | Description |
|---|---|---|
| `id` | TEXT (UUID) | Primary key. Assigned in the Domain layer at session creation (§4), not database-generated. |
| `started_at` | INTEGER (Unix epoch ms) | Session start timestamp. |
| `ended_at` | INTEGER (Unix epoch ms), nullable | Null while a session is active; set on completion. |
| `device_identifier` | TEXT | Identifier of the camera device used (`02` FR-501). |
| `mean_heart_rate_bpm` | REAL, nullable | Computed on session end; null if the session ended before any `STABLE` estimate occurred. |
| `mean_confidence` | REAL, nullable | Computed on session end, over the same window as `mean_heart_rate_bpm`. |
| `status` | TEXT | One of `completed`, `interrupted` — distinguishing a normal end (`02` FR-107) from an abnormal termination detected on next launch. |
| `app_version` | TEXT | The application version that recorded the session — category (c) metadata per `02 §5` DR-3. |

### 3.2 `heart_rate_samples`

One row per one-second estimate (`08 §2`'s update cadence) produced during a session — category (b) derived signal data.

| Column | Type | Description |
|---|---|---|
| `id` | INTEGER (autoincrement) | Primary key; database-assigned is acceptable here since these rows have no identity meaning outside their session, unlike `MeasurementSession` itself. |
| `session_id` | TEXT | Foreign key to `sessions.id`. |
| `offset_seconds` | REAL | Time offset from `started_at`, not an absolute timestamp — keeps the row independent of clock changes mid-session. |
| `bpm` | REAL, nullable | Null for a sample recorded while `SignalQuality` was `SEARCHING` or `DEGRADED` (`08 §4`) — no estimate existed at that moment, and the schema represents that honestly rather than inventing a placeholder value. |
| `confidence` | REAL, nullable | Same nullability rule as `bpm`. |
| `signal_quality` | TEXT | One of `SEARCHING`, `STABLE`, `DEGRADED` (`08 §4`) — retained even for null-`bpm` rows, since the trend view (`06 §6.4`) needs to render gaps meaningfully rather than as zero. |

### 3.3 `schema_version`

| Column | Type | Description |
|---|---|---|
| `version` | INTEGER | The currently applied schema version. Single-row table. |
| `applied_at` | INTEGER (Unix epoch ms) | When this version was applied. |

---

## 4. Identity Strategy

`MeasurementSession.id` is a UUID, generated in the Domain layer at the moment a session is created — not an autoincrement value assigned by SQLite on insert. This matters architecturally, not just stylistically: an Entity (`03 §3` names `MeasurementSession` as one, deliberately, not a Value Object) needs a stable identity from the moment it exists in memory, including before it has ever been persisted — `LiveMeasurementOrchestrator` (`03 §6.2`) refers to the session by its ID throughout its lifetime, well before `EndMeasurementSessionUseCase` ever calls `MeasurementRepository`. A database-assigned identity would leave the in-memory entity without a real identity until that first write, which is backwards.

---

## 5. Migration Strategy

No migration framework (Flyway, Liquibase, or similar) is added to the approved dependency set (`00 §16`) for this — a single-table-pair schema at V1 scale does not justify a new dependency and its associated ADR. Instead: `SqliteMeasurementRepository`, on startup, reads `schema_version`, compares it against the version the running application expects, and applies any pending numbered DDL scripts (`04 §6`'s `schema/*.sql` resource location) in order, incrementing `schema_version` after each one succeeds. This is deliberately minimal — a hand-rolled, single-purpose runner, not a general migration engine — and is revisited via `00 §8`'s decision process if the schema ever grows complex enough to need one.

---

## 6. Transaction and Consistency Guarantees

Realizing `02` NFR-402 ("a session interrupted by an application crash SHALL NOT corrupt previously persisted session records"):

- A session's `sessions` row and all of its `heart_rate_samples` rows are written within a **single SQLite transaction**, committed only after every row has been written successfully. A crash or failure partway through leaves the previous state intact (the transaction never commits) rather than a half-written session.
- `heart_rate_samples` rows are **not** written one at a time as each one-second estimate arrives during a live session — they are buffered in memory by `LiveMeasurementOrchestrator` (`03 §6.2`) and written in the single end-of-session transaction described above. This avoids 300+ individual disk writes per five-minute session and keeps the "session interrupted" failure mode simple: either the whole session persists, or none of it does, with no partially-written history entry to reason about.
- A session that never reaches `EndMeasurementSessionUseCase` (application crash mid-session) is not present in `sessions` at all on next launch — there is nothing to mark `interrupted` retroactively for data that was never written, which is a deliberate consequence of the buffer-then-commit design, not an oversight.

---

## 7. Connection Management and SQLite Configuration

- A single, long-lived JDBC connection is held for the application's lifetime, consistent with the single-user, single-process usage pattern (`01 §10` A4) — no connection pool is needed at this scale, and introducing one would be unjustified complexity.
- **Write-Ahead Logging (WAL) mode** is enabled on the database connection. WAL allows concurrent readers (e.g., `ListSessionHistoryUseCase` querying `sessions` while a live session's data is buffered) without blocking on a writer, which matters here because a history-list read could otherwise be blocked behind an in-progress live session that hasn't committed yet.
- The connection is closed deterministically on application exit via the composition root (`03 §8`, `rppg-app`), consistent with `00 §25`'s executor-shutdown discipline extended to this resource.

---

## 8. File Location

The SQLite database file lives in a platform-appropriate, user-local application-data directory, satisfying `02` NFR-502's "user SHALL be able to locate and manually delete this file outside the application":

| Platform | Location |
|---|---|
| Windows | `%APPDATA%\rppgvitals\vitals.db` |
| macOS | `~/Library/Application Support/rppgvitals/vitals.db` |
| Linux | `~/.local/share/rppgvitals/vitals.db` (following the XDG Base Directory convention) |

No automated backup or cloud sync is implemented — consistent with `00 §4`'s NG-3 (no cloud backend) and `01 §5`'s Persona B expectation of a fully local tool, loss of this file (e.g., the user deletes it manually) is an accepted, understood consequence of local-only storage, not a defect to engineer around.

---

## 9. Repository Query Patterns

Mapping `MeasurementRepository`'s responsibilities (`03 §4`) to the requirements that motivate them:

| Operation | Requirement | Query Shape |
|---|---|---|
| Save a completed session | `02` FR-201, FR-107 | Single transaction: one insert into `sessions`, batch insert into `heart_rate_samples` (§6). |
| List session summaries | `02` FR-202 | Select from `sessions` ordered by `started_at` descending — `heart_rate_samples` is not joined for the list view, keeping it cheap regardless of history size. |
| Get session detail | `02` FR-203 | Select the one `sessions` row by `id`, plus all `heart_rate_samples` rows for that `session_id` ordered by `offset_seconds`, for the trend chart (`06 §6.4`). |
| Delete a session | `02` FR-204 | Single transaction: delete the `sessions` row and its `heart_rate_samples` rows together — never leave orphaned sample rows behind. |

---

## 10. Data Retention

No row in either table is auto-expired or auto-deleted by the application, per `02 §5` DR-3 ("the system SHALL NOT auto-expire history in V1"). Deletion happens exclusively through the explicit, user-initiated action in `02` FR-204, or through the user manually removing the database file described in §8 — there is no third path.

---

## 11. Relationship to Other Documents

| Document | What It Inherits From This Document |
|---|---|
| `12_PERFORMANCE.md` | The end-of-session batch-write pattern (§6) and WAL configuration (§7) are the concrete persistence behavior that document's I/O performance targets must account for. |
| `14_DEPLOYMENT.md` | The file locations in §8 are what the installer must ensure are writable on first run. |

---

## 12. Revision History

| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-07-12 | Initial ratified version, derived from `03_ARCHITECTURE.md` v1.0.0 and `02_SOFTWARE_REQUIREMENT.md §5`. |

---

*End of 10_DATABASE.md. Subordinate to `02_SOFTWARE_REQUIREMENT.md` and `03_ARCHITECTURE.md`; binding on all documents listed in §11.*