package id.asr.rppgvitals.domain.estimation;

import java.util.Objects;

import id.asr.rppgvitals.domain.signal.PpgWaveform;

/// The POS (Plane-Orthogonal-to-Skin) `SignalEstimator` (`07_SIGNAL_PROCESSING.md §8`; Wang et al.,
/// 2017).
///
/// From the window-normalised traces it forms two projected signals `Xs = g − b` and
/// `Ys = −2r + g + b`, weights them by `α = σ(Xs) / σ(Ys)`, and combines them into the pulse signal
/// `S = Xs + α·Ys`, which is then bandpass-filtered (`07 §9`) and handed to frequency analysis. POS
/// projects out intensity/motion variation by construction and is the most motion-robust of the
/// three, at the highest computational cost (`07 §10`).
///
/// Domain Service (`03_ARCHITECTURE.md §4`), stateless and therefore thread-safe.
public final class PosSignalEstimator implements SignalEstimator {

    private static final String ALGORITHM = "POS";

    private final PulseSpectrumAnalyzer analyzer = new PulseSpectrumAnalyzer();

    /// Creates a POS estimator.
    public PosSignalEstimator() {}

    /// {@inheritDoc}
    @Override
    public HeartRateEstimate estimate(PpgWaveform waveform) {
        Objects.requireNonNull(waveform, "waveform");
        double[][] channels = EstimatorSupport.channels(waveform);
        double[] red = EstimatorSupport.normalize(channels[0], ALGORITHM);
        double[] green = EstimatorSupport.normalize(channels[1], ALGORITHM);
        double[] blue = EstimatorSupport.normalize(channels[2], ALGORITHM);
        double[] pulse = projectAndCombine(red, green, blue);

        double sampleRateHz = waveform.samplingRateHz();
        double[] filtered = EstimatorSupport.bandpassFilter(sampleRateHz).filtfilt(pulse);
        return analyzer.estimate(filtered, sampleRateHz, EstimatorSupport.latestTimestamp(waveform), ALGORITHM);
    }

    private static double[] projectAndCombine(double[] red, double[] green, double[] blue) {
        int n = red.length;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = green[i] - blue[i];
            ys[i] = -2.0 * red[i] + green[i] + blue[i];
        }
        double alpha = EstimatorSupport.alpha(xs, ys, ALGORITHM);
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = xs[i] + alpha * ys[i];
        }
        return signal;
    }
}
