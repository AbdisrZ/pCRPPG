package id.asr.rppgvitals.domain.estimation;

import id.asr.rppgvitals.domain.exception.SignalQualityException;
import id.asr.rppgvitals.domain.signal.PpgWaveform;

/// Domain port for heart-rate estimation from an rPPG waveform (`03_ARCHITECTURE.md §4`).
///
/// Unlike the other three ports, `SignalEstimator` is implemented within the domain itself as a set
/// of Domain Services (`GreenChannelSignalEstimator`, `ChromSignalEstimator`, `PosSignalEstimator`)
/// depending only on Apache Commons Math — the Strategy point of `00_MASTER_PROMPT.md §30`. A new
/// algorithm is added as a new implementation, never by branching an existing one (`00 §28`,
/// Open/Closed).
///
/// A "no reliable estimate yet" outcome is expressed as a low-confidence [HeartRateEstimate], not an
/// exception; exceptions are reserved for genuinely anomalous estimation math (`08_ESTIMATOR_ENGINE.md §5`).
public interface SignalEstimator {

    /// Estimates the heart rate represented by the given waveform.
    ///
    /// @param waveform the analysis window to estimate from; never `null`
    /// @return the heart-rate estimate, whose confidence conveys how reliable it is
    /// @throws SignalQualityException if the waveform is numerically degenerate for this algorithm
    ///     (for example a zero-variance window that would divide by zero in the weighting step)
    HeartRateEstimate estimate(PpgWaveform waveform);
}
