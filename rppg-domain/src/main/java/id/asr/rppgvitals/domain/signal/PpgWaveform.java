package id.asr.rppgvitals.domain.signal;

import java.util.List;
import java.util.Objects;

/// An ordered, bounded window of [PpgSample]s ready for heart-rate estimation
/// (`03_ARCHITECTURE.md §3`).
///
/// The waveform carries its own sampling rate so that downstream frequency analysis can map FFT bins
/// to hertz without assuming a fixed capture rate (`08_ESTIMATOR_ENGINE.md §2`). The sample list is
/// defensively copied on construction and exposed as an immutable list, per `05_CODING_STANDARD.md §6`.
///
/// @param samples the ordered samples in the window, oldest first; never `null` or empty
/// @param samplingRateHz the sampling rate of the window in hertz; strictly positive
public record PpgWaveform(List<PpgSample> samples, double samplingRateHz) {

    /// Copies the sample list defensively and validates the window.
    public PpgWaveform {
        Objects.requireNonNull(samples, "samples");
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("samples must not be empty");
        }
        if (!(samplingRateHz > 0.0) || !Double.isFinite(samplingRateHz)) {
            throw new IllegalArgumentException("samplingRateHz must be positive and finite, was " + samplingRateHz);
        }
        samples = List.copyOf(samples);
    }

    /// Returns the number of samples in the window.
    ///
    /// @return the window length in samples
    public int size() {
        return samples.size();
    }

    /// Returns the time span the window covers, derived from its length and sampling rate.
    ///
    /// @return the window duration in seconds
    public double durationSeconds() {
        return samples.size() / samplingRateHz;
    }
}
