package id.asr.rppgvitals.domain.estimation;

import java.util.Objects;

import id.asr.rppgvitals.domain.signal.PpgWaveform;

/// The green-channel `SignalEstimator` (`07_SIGNAL_PROCESSING.md §6`; Verkruysse et al., 2008).
///
/// The simplest estimator and the baseline/fallback of the three: the temporally normalised green
/// trace is used directly as the candidate pulse signal, bandpass-filtered (`07 §9`), then handed to
/// frequency analysis. No cross-channel combination is performed, so it offers no motion-artifact
/// suppression — the red and blue channels that CHROM and POS use to cancel illumination and motion
/// artifacts are discarded (`07 §6`).
///
/// Domain Service (`03_ARCHITECTURE.md §4`), stateless and therefore thread-safe.
public final class GreenChannelSignalEstimator implements SignalEstimator {

    private static final String ALGORITHM = "GREEN";

    private final PulseSpectrumAnalyzer analyzer = new PulseSpectrumAnalyzer();

    /// Creates a green-channel estimator.
    public GreenChannelSignalEstimator() {}

    /// {@inheritDoc}
    @Override
    public HeartRateEstimate estimate(PpgWaveform waveform) {
        Objects.requireNonNull(waveform, "waveform");
        double[][] channels = EstimatorSupport.channels(waveform);
        double[] green = EstimatorSupport.normalize(channels[1], ALGORITHM);
        EstimatorSupport.requireVariation(green, ALGORITHM);
        double sampleRateHz = waveform.samplingRateHz();
        double[] pulse = EstimatorSupport.bandpassFilter(sampleRateHz).filtfilt(green);
        return analyzer.estimate(pulse, sampleRateHz, EstimatorSupport.latestTimestamp(waveform), ALGORITHM);
    }
}
