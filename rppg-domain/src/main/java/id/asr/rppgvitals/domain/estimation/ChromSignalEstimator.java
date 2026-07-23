package id.asr.rppgvitals.domain.estimation;

import java.util.Objects;

import id.asr.rppgvitals.domain.signal.PpgWaveform;

/// The CHROM (chrominance-based) `SignalEstimator` (`07_SIGNAL_PROCESSING.md §7`; De Haan & Jeanne,
/// 2013). Ships as the V1 default estimator (`08_ESTIMATOR_ENGINE.md §6`).
///
/// Unlike Green and POS, CHROM applies the bandpass filter to each normalised channel *before* the
/// chrominance combination (`07 §7`): from the filtered traces `y_r, y_g, y_b` it forms
/// `A = 3·y_r − 2·y_g` and `B = 1.5·y_r + y_g − 1.5·y_b`, weights them by `α = σ(A) / σ(B)`, and
/// combines them into `S = 3·(1 − α/2)·y_r − 2·(1 + α/2)·y_g + (3α/2)·y_b`. The combination is
/// designed so specular reflection and motion — which affect all channels similarly — cancel while
/// the pulsatile component survives.
///
/// Domain Service (`03_ARCHITECTURE.md §4`), stateless and therefore thread-safe.
public final class ChromSignalEstimator implements SignalEstimator {

    private static final String ALGORITHM = "CHROM";

    private final PulseSpectrumAnalyzer analyzer = new PulseSpectrumAnalyzer();

    /// Creates a CHROM estimator.
    public ChromSignalEstimator() {}

    /// {@inheritDoc}
    @Override
    public HeartRateEstimate estimate(PpgWaveform waveform) {
        Objects.requireNonNull(waveform, "waveform");
        double[][] channels = EstimatorSupport.channels(waveform);
        double sampleRateHz = waveform.samplingRateHz();
        ButterworthBandpassFilter filter = EstimatorSupport.bandpassFilter(sampleRateHz);

        double[] filteredRed = filter.filtfilt(EstimatorSupport.normalize(channels[0], ALGORITHM));
        double[] filteredGreen = filter.filtfilt(EstimatorSupport.normalize(channels[1], ALGORITHM));
        double[] filteredBlue = filter.filtfilt(EstimatorSupport.normalize(channels[2], ALGORITHM));
        double[] pulse = chrominanceSignal(filteredRed, filteredGreen, filteredBlue);

        return analyzer.estimate(pulse, sampleRateHz, EstimatorSupport.latestTimestamp(waveform), ALGORITHM);
    }

    private static double[] chrominanceSignal(double[] red, double[] green, double[] blue) {
        int n = red.length;
        double[] chrominanceA = new double[n];
        double[] chrominanceB = new double[n];
        for (int i = 0; i < n; i++) {
            chrominanceA[i] = 3.0 * red[i] - 2.0 * green[i];
            chrominanceB[i] = 1.5 * red[i] + green[i] - 1.5 * blue[i];
        }
        double alpha = EstimatorSupport.alpha(chrominanceA, chrominanceB, ALGORITHM);
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = 3.0 * (1.0 - alpha / 2.0) * red[i]
                    - 2.0 * (1.0 + alpha / 2.0) * green[i]
                    + (3.0 * alpha / 2.0) * blue[i];
        }
        return signal;
    }
}
