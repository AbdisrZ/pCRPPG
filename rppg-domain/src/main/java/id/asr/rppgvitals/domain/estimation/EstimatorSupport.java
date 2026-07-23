package id.asr.rppgvitals.domain.estimation;

import java.time.Instant;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import id.asr.rppgvitals.domain.exception.SignalQualityException;
import id.asr.rppgvitals.domain.signal.PpgSample;
import id.asr.rppgvitals.domain.signal.PpgWaveform;

/// Shared math used by the `SignalEstimator` implementations (`07_SIGNAL_PROCESSING.md §5`, §7, §8):
/// per-channel extraction, temporal normalisation, and the standard-deviation alpha weighting common
/// to CHROM and POS.
///
/// Package-private, stateless, pure functions — extracted here so the three estimators compose the
/// same building blocks rather than duplicating them (`00_MASTER_PROMPT.md §5`).
final class EstimatorSupport {

    /// The shared Butterworth passband and order (`07 §9`).
    static final double MIN_HZ = 0.7;

    static final double MAX_HZ = 2.5;
    static final int FILTER_ORDER = 3;

    private EstimatorSupport() {}

    /// Builds the shared zero-phase Butterworth bandpass filter for a given sampling rate (`07 §9`).
    ///
    /// @param sampleRateHz the sampling rate in hertz; strictly positive and above 5 Hz
    /// @return a filter designed for the 0.7-2.5 Hz passband at that rate
    static ButterworthBandpassFilter bandpassFilter(double sampleRateHz) {
        return new ButterworthBandpassFilter(FILTER_ORDER, MIN_HZ, MAX_HZ, sampleRateHz);
    }

    /// Returns the timestamp to stamp on an estimate: that of the most recent sample in the window.
    ///
    /// @param waveform the analysis window; never `null`
    /// @return the timestamp of the last sample
    static Instant latestTimestamp(PpgWaveform waveform) {
        List<PpgSample> samples = waveform.samples();
        return samples.get(samples.size() - 1).timestamp();
    }

    /// Extracts the three RGB channel traces from a waveform, oldest sample first.
    ///
    /// @param waveform the analysis window; never `null`
    /// @return a `[3][n]` array holding the red, green, and blue traces in that order
    static double[][] channels(PpgWaveform waveform) {
        var samples = waveform.samples();
        int n = samples.size();
        double[] red = new double[n];
        double[] green = new double[n];
        double[] blue = new double[n];
        for (int i = 0; i < n; i++) {
            PpgSample sample = samples.get(i);
            red[i] = sample.red();
            green[i] = sample.green();
            blue[i] = sample.blue();
        }
        return new double[][] {red, green, blue};
    }

    /// Temporally normalises a channel by dividing every sample by the channel's window mean
    /// (`07 §5`), which is what makes the algorithms insensitive to skin tone and slow illumination.
    ///
    /// @param channel the channel trace to normalise; never `null`
    /// @param algorithm the calling estimator's identifier, for error context
    /// @return a new array holding the normalised trace
    /// @throws SignalQualityException if the channel mean is zero (a degenerate all-dark window)
    static double[] normalize(double[] channel, String algorithm) {
        double sum = 0.0;
        for (double value : channel) {
            sum += value;
        }
        double mean = sum / channel.length;
        if (mean == 0.0) {
            throw new SignalQualityException(algorithm, "channel window mean is zero; cannot normalise");
        }
        double[] normalized = new double[channel.length];
        for (int i = 0; i < channel.length; i++) {
            normalized[i] = channel[i] / mean;
        }
        return normalized;
    }

    /// Requires that a signal has non-zero variance, guarding the estimators against a degenerate
    /// constant (zero-variance) window that carries no pulse information (`08_ESTIMATOR_ENGINE.md §5`).
    ///
    /// @param signal the signal to check; never `null`
    /// @param algorithm the calling estimator's identifier, for error context
    /// @throws SignalQualityException if every sample is identical (zero variance)
    static void requireVariation(double[] signal, String algorithm) {
        if (new StandardDeviation().evaluate(signal) == 0.0) {
            throw new SignalQualityException(algorithm, "zero-variance window carries no pulse signal");
        }
    }

    /// Computes the alpha weight as the ratio of the standard deviations of two signals
    /// (`07 §7`, `07 §8`).
    ///
    /// @param numerator the signal whose standard deviation is the numerator; never `null`
    /// @param denominator the signal whose standard deviation is the denominator; never `null`
    /// @param algorithm the calling estimator's identifier, for error context
    /// @return the ratio `σ(numerator) / σ(denominator)`
    /// @throws SignalQualityException if the denominator has zero variance (a degenerate window)
    static double alpha(double[] numerator, double[] denominator, String algorithm) {
        double denominatorSd = new StandardDeviation().evaluate(denominator);
        if (denominatorSd == 0.0) {
            throw new SignalQualityException(algorithm, "zero-variance window in alpha weighting");
        }
        return new StandardDeviation().evaluate(numerator) / denominatorSd;
    }
}
