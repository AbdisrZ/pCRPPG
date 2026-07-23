package id.asr.rppgvitals.domain.session;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;

/// A lightweight projection of a [MeasurementSession] for history listing (`03_ARCHITECTURE.md §3`,
/// `02_SOFTWARE_REQUIREMENT.md` FR-202). Its fields mirror one `sessions` row (`10_DATABASE.md §3.1`)
/// so the history list is a cheap select with no join to `heart_rate_samples` (`10 §9`).
///
/// `meanHeartRateBpm` and `meanConfidence` are boxed and nullable: a completed session that never
/// produced an estimate has no mean (`10 §3.1`). Nullable boxed fields are used deliberately here at
/// the persistence boundary, the exception `05_CODING_STANDARD.md §7` permits.
///
/// @param sessionId the identity of the summarised session; never `null`
/// @param startedAt the instant the session started; never `null`
/// @param endedAt the instant the session ended, or `null` if it is still active
/// @param status the session's lifecycle status; never `null`
/// @param meanHeartRateBpm the mean of the session's estimates in bpm, or `null` if it produced none;
///     finite and non-negative when present
/// @param meanConfidence the mean confidence of the session's estimates, or `null` if it produced
///     none; within `[0, 1]` when present
public record SessionSummary(
        UUID sessionId,
        Instant startedAt,
        Instant endedAt,
        SessionStatus status,
        Double meanHeartRateBpm,
        Double meanConfidence) {

    /// Validates the projection.
    public SessionSummary {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(status, "status");
        if (meanHeartRateBpm != null && (!Double.isFinite(meanHeartRateBpm) || meanHeartRateBpm < 0.0)) {
            throw new IllegalArgumentException(
                    "meanHeartRateBpm must be finite and non-negative, was " + meanHeartRateBpm);
        }
        if (meanConfidence != null && !(meanConfidence >= 0.0 && meanConfidence <= 1.0)) {
            throw new IllegalArgumentException("meanConfidence must be in [0, 1], was " + meanConfidence);
        }
    }

    /// Projects a full session down to a summary, computing the mean rate and confidence over its
    /// recorded estimates.
    ///
    /// @param session the session to summarise; never `null`
    /// @return the corresponding summary, with `null` means when the session recorded no estimates
    public static SessionSummary from(MeasurementSession session) {
        Objects.requireNonNull(session, "session");
        List<HeartRateEstimate> estimates = session.estimates();
        OptionalDouble meanBpm = estimates.stream()
                .mapToDouble(HeartRateEstimate::beatsPerMinute)
                .average();
        OptionalDouble meanConfidence =
                estimates.stream().mapToDouble(HeartRateEstimate::confidence).average();
        return new SessionSummary(
                session.id(),
                session.startedAt(),
                session.endedAt().orElse(null),
                session.status(),
                meanBpm.isPresent() ? meanBpm.getAsDouble() : null,
                meanConfidence.isPresent() ? meanConfidence.getAsDouble() : null);
    }
}
