-- Initial schema for the rPPG Desktop Vitals Monitor (10_DATABASE.md sec 3), schema version 1.
-- Stores only category (b) derived signal data and category (c) session metadata; no raw-frame or
-- pixel/BLOB column exists anywhere, enforcing 02_SOFTWARE_REQUIREMENT.md sec 5 structurally.

CREATE TABLE sessions (
    id                  TEXT    NOT NULL PRIMARY KEY,
    started_at          INTEGER NOT NULL,
    ended_at            INTEGER,
    device_identifier   TEXT    NOT NULL,
    mean_heart_rate_bpm REAL,
    mean_confidence     REAL,
    status              TEXT    NOT NULL,
    app_version         TEXT    NOT NULL
);

CREATE TABLE heart_rate_samples (
    id             INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    session_id     TEXT    NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    offset_seconds REAL    NOT NULL,
    bpm            REAL,
    confidence     REAL,
    signal_quality TEXT    NOT NULL
);

CREATE INDEX idx_heart_rate_samples_session ON heart_rate_samples (session_id);
