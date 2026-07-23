package id.asr.rppgvitals.domain.signal;

import java.util.Objects;

/// The domain's encoding of live-signal quality during a measurement session
/// (`03_ARCHITECTURE.md §3`, `§6.1`). A sealed type over three states so that the presentation layer
/// can handle it exhaustively with a pattern-matched `switch` and no `default` branch
/// (`05_CODING_STANDARD.md §6`).
///
/// The authoritative transition rules between these states are owned by `08_ESTIMATOR_ENGINE.md §4`
/// and implemented in a later task (T-108); this type only models the states themselves.
///
/// - [Searching] — acquiring or re-acquiring a usable signal; no reliable estimate yet.
/// - [Stable] — a reliable estimate is being produced (maps to the session `Reporting` state of
///   `02_SOFTWARE_REQUIREMENT.md §3.4`).
/// - [Degraded] — measurement is interrupted by an operational condition (camera lost, insufficient
///   lighting, ROI lost); carries a human-oriented reason.
public sealed interface SignalQuality permits SignalQuality.Searching, SignalQuality.Stable, SignalQuality.Degraded {

    /// The state in which the pipeline is acquiring or re-acquiring a usable signal.
    record Searching() implements SignalQuality {}

    /// The state in which a reliable heart-rate estimate is being produced.
    record Stable() implements SignalQuality {}

    /// The state in which measurement is interrupted by a recoverable operational condition.
    ///
    /// @param reason a short, human-oriented description of what degraded the signal; never `null` or blank
    record Degraded(String reason) implements SignalQuality {

        /// Validates the degradation reason.
        public Degraded {
            Objects.requireNonNull(reason, "reason");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }
}
