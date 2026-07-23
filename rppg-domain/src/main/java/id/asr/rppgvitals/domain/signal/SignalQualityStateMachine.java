package id.asr.rppgvitals.domain.signal;

import java.util.Objects;

/// The authoritative [SignalQuality] state machine of `08_ESTIMATOR_ENGINE.md §4`.
///
/// The transitions are pure functions of the current state and the triggering event, so this type is
/// stateless and thread-safe; the caller (the `LiveMeasurementOrchestrator`, `11_THREADING.md`) holds
/// the current [SignalQuality] and advances it on each event:
///
/// - a new estimate drives [Searching][SignalQuality.Searching] ↔ [Stable][SignalQuality.Stable] by
///   its confidence (via [#afterEstimate]);
/// - a degrading operational condition — camera lost, insufficient lighting, ROI lost — moves any
///   state to [Degraded][SignalQuality.Degraded] (via [#afterDegradingCondition]); that condition is
///   detected upstream and translated by the orchestrator (`03_ARCHITECTURE.md §7.2`), never thrown
///   from the estimator (`08 §5`);
/// - resolving that condition returns [Degraded][SignalQuality.Degraded] to
///   [Searching][SignalQuality.Searching] (via [#afterConditionResolved]).
public final class SignalQualityStateMachine {

    /// The confidence at or above which the signal is treated as [Stable][SignalQuality.Stable]; below
    /// it the estimate is below the display floor and the state is [Searching][SignalQuality.Searching]
    /// (`08_ESTIMATOR_ENGINE.md §3`, `§4`).
    public static final double STABILITY_THRESHOLD = 0.3;

    /// Creates a state machine. Instances carry no state and may be shared.
    public SignalQualityStateMachine() {}

    /// Returns the initial state of a session, before any estimate has been produced.
    ///
    /// @return [Searching][SignalQuality.Searching]
    public SignalQuality initial() {
        return new SignalQuality.Searching();
    }

    /// Advances the state when a new estimate is produced.
    ///
    /// @param current the current signal-quality state; never `null`
    /// @param confidence the confidence of the new estimate, in `[0, 1]`
    /// @param windowComplete whether the analysis window was full enough to produce a real estimate
    /// @return [Degraded][SignalQuality.Degraded] unchanged if currently degraded (that state is left
    ///     only by [#afterConditionResolved]); otherwise [Stable][SignalQuality.Stable] when the window
    ///     is complete and confidence is at least [#STABILITY_THRESHOLD], else
    ///     [Searching][SignalQuality.Searching]
    public SignalQuality afterEstimate(SignalQuality current, double confidence, boolean windowComplete) {
        Objects.requireNonNull(current, "current");
        if (current instanceof SignalQuality.Degraded) {
            return current;
        }
        if (!windowComplete || confidence < STABILITY_THRESHOLD) {
            return new SignalQuality.Searching();
        }
        return new SignalQuality.Stable();
    }

    /// Moves to [Degraded][SignalQuality.Degraded] because of an operational condition.
    ///
    /// @param reason a short, human-oriented description of the condition; never `null` or blank
    /// @return a [Degraded][SignalQuality.Degraded] state carrying the reason
    public SignalQuality afterDegradingCondition(String reason) {
        return new SignalQuality.Degraded(reason);
    }

    /// Leaves [Degraded][SignalQuality.Degraded] once the underlying condition is resolved.
    ///
    /// @param current the current signal-quality state; never `null`
    /// @return [Searching][SignalQuality.Searching] if currently degraded, otherwise `current` unchanged
    public SignalQuality afterConditionResolved(SignalQuality current) {
        Objects.requireNonNull(current, "current");
        return current instanceof SignalQuality.Degraded ? new SignalQuality.Searching() : current;
    }
}
