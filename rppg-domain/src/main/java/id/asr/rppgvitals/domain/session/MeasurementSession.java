package id.asr.rppgvitals.domain.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;

/// A single measurement session (`03_ARCHITECTURE.md §3`). This is the domain's only Entity: it has
/// stable identity and mutable state that evolves over the session's life, so it is an explicit class
/// with identity-based equality rather than a record (`05_CODING_STANDARD.md §6`).
///
/// A session starts [SessionStatus#ACTIVE], accumulates [HeartRateEstimate]s as they are produced,
/// and reaches a terminal [SessionStatus#COMPLETED] or [SessionStatus#ABORTED] when it ends.
///
/// **Thread-safety.** This type is not thread-safe. It is confined to a single owning thread — the
/// estimation stage of the `LiveMeasurementOrchestrator` (`11_THREADING.md`) — which is the only
/// writer; it is not shared for concurrent mutation (`00_MASTER_PROMPT.md §25`).
public final class MeasurementSession {

    private final UUID id;
    private final String deviceIdentifier;
    private final Instant startedAt;
    private final List<HeartRateEstimate> estimates;
    private Instant endedAt;
    private SessionStatus status;

    /// Starts a new, active session.
    ///
    /// @param id the stable identity of the session; never `null`
    /// @param deviceIdentifier the identifier of the camera device the session runs against
    ///     (`10_DATABASE.md §3.1`); never `null` or blank
    /// @param startedAt the instant the session started; never `null`
    public MeasurementSession(UUID id, String deviceIdentifier, Instant startedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.deviceIdentifier = Objects.requireNonNull(deviceIdentifier, "deviceIdentifier");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        if (deviceIdentifier.isBlank()) {
            throw new IllegalArgumentException("deviceIdentifier must not be blank");
        }
        this.estimates = new ArrayList<>();
        this.status = SessionStatus.ACTIVE;
    }

    /// Appends a newly produced estimate to the session.
    ///
    /// @param estimate the estimate to record; never `null`
    /// @throws IllegalStateException if the session has already ended
    public void recordEstimate(HeartRateEstimate estimate) {
        Objects.requireNonNull(estimate, "estimate");
        requireActive();
        estimates.add(estimate);
    }

    /// Ends the session normally.
    ///
    /// @param endedAt the instant the session ended; never `null` and not before the start
    /// @throws IllegalStateException if the session has already ended
    public void complete(Instant endedAt) {
        end(endedAt, SessionStatus.COMPLETED);
    }

    /// Ends the session abnormally.
    ///
    /// @param endedAt the instant the session ended; never `null` and not before the start
    /// @throws IllegalStateException if the session has already ended
    public void abort(Instant endedAt) {
        end(endedAt, SessionStatus.ABORTED);
    }

    private void end(Instant when, SessionStatus terminalStatus) {
        Objects.requireNonNull(when, "endedAt");
        requireActive();
        if (when.isBefore(startedAt)) {
            throw new IllegalArgumentException("endedAt must not be before startedAt");
        }
        this.endedAt = when;
        this.status = terminalStatus;
    }

    private void requireActive() {
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("session has already ended with status " + status);
        }
    }

    /// Returns the stable identity of the session.
    ///
    /// @return the session id
    public UUID id() {
        return id;
    }

    /// Returns the identifier of the camera device the session runs against.
    ///
    /// @return the device identifier
    public String deviceIdentifier() {
        return deviceIdentifier;
    }

    /// Returns the instant the session started.
    ///
    /// @return the start instant
    public Instant startedAt() {
        return startedAt;
    }

    /// Returns the instant the session ended, if it has ended.
    ///
    /// @return the end instant, or [Optional#empty()] while the session is still active
    public Optional<Instant> endedAt() {
        return Optional.ofNullable(endedAt);
    }

    /// Returns the current lifecycle status.
    ///
    /// @return the session status
    public SessionStatus status() {
        return status;
    }

    /// Returns the estimates accumulated so far, oldest first.
    ///
    /// @return an unmodifiable snapshot of the recorded estimates
    public List<HeartRateEstimate> estimates() {
        return List.copyOf(estimates);
    }

    /// Returns the most recently recorded estimate, if any.
    ///
    /// @return the latest estimate, or [Optional#empty()] if none have been recorded
    public Optional<HeartRateEstimate> latestEstimate() {
        return estimates.isEmpty() ? Optional.empty() : Optional.of(estimates.get(estimates.size() - 1));
    }

    /// Two sessions are equal exactly when they share the same identity.
    ///
    /// @param other the object to compare with
    /// @return `true` if `other` is a `MeasurementSession` with the same id
    @Override
    public boolean equals(Object other) {
        return other instanceof MeasurementSession session && id.equals(session.id);
    }

    /// Returns a hash code consistent with identity-based equality.
    ///
    /// @return the hash of the session id
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /// Returns a concise, non-sensitive description of the session.
    ///
    /// @return a string with the id, status, and estimate count
    @Override
    public String toString() {
        return "MeasurementSession[id=%s, status=%s, estimates=%d]".formatted(id, status, estimates.size());
    }
}
