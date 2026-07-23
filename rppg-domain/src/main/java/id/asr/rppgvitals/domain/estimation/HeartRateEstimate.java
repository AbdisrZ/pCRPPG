package id.asr.rppgvitals.domain.estimation;

import java.time.Instant;
import java.util.Objects;

/// A computed heart-rate estimate with its confidence and provenance (`03_ARCHITECTURE.md §3`).
///
/// Produced by a [SignalEstimator] from a [id.asr.rppgvitals.domain.signal.PpgWaveform]. The
/// confidence is the aggregate three-component score of `08_ESTIMATOR_ENGINE.md §3`; the algorithm
/// identifier records which estimator produced the value, so competing estimators (Green / CHROM /
/// POS) remain distinguishable downstream without an enumerated type (`00_MASTER_PROMPT.md §28`,
/// Open/Closed).
///
/// @param beatsPerMinute the estimated heart rate in beats per minute; finite and never negative
/// @param confidence the aggregate confidence in the estimate, in the closed range `[0, 1]`
/// @param timestamp the instant this estimate was produced; never `null`
/// @param algorithm the identifier of the estimator that produced it; never `null` or blank
public record HeartRateEstimate(double beatsPerMinute, double confidence, Instant timestamp, String algorithm) {

    /// Validates the estimate's ranges and provenance.
    public HeartRateEstimate {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(algorithm, "algorithm");
        if (algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm must not be blank");
        }
        if (!Double.isFinite(beatsPerMinute) || beatsPerMinute < 0.0) {
            throw new IllegalArgumentException("beatsPerMinute must be finite and non-negative, was " + beatsPerMinute);
        }
        if (!(confidence >= 0.0 && confidence <= 1.0)) {
            throw new IllegalArgumentException("confidence must be in [0, 1], was " + confidence);
        }
    }
}
