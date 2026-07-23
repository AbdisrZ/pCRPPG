/// Domain representation of the extracted rPPG signal and its quality state.
///
/// Holds the `PpgSample` and `PpgWaveform` value objects and the sealed `SignalQuality`
/// state type (`SEARCHING` / `STABLE` / `DEGRADED`) whose transition rules are governed by
/// `08_ESTIMATOR_ENGINE.md §4`. This is pure signal representation — extraction algorithms
/// live in `domain.estimation`, which depends inward on the types defined here. Governed by
/// `03_ARCHITECTURE.md §3` and `07_SIGNAL_PROCESSING.md`.
package id.asr.rppgvitals.domain.signal;
