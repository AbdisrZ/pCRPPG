/// Heart-rate estimation from a windowed rPPG waveform.
///
/// Owns the [SignalEstimator] port and its three Domain-Service implementations
/// (`GreenChannelSignalEstimator`, `ChromSignalEstimator`, `PosSignalEstimator` — the
/// Strategy point of `00 §30`), plus the `HeartRateEstimate` value object. Depends only on
/// `domain.signal` and Apache Commons Math; fully unit-testable with no adapters attached
/// (`03 §4`). Governed by `07_SIGNAL_PROCESSING.md §6–§8` and `08_ESTIMATOR_ENGINE.md`.
package id.asr.rppgvitals.domain.estimation;
