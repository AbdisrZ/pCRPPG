package id.asr.rppgvitals.domain.estimation;

import java.time.Instant;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import id.asr.rppgvitals.domain.exception.SignalQualityException;

/// Converts a filtered pulse signal into a heart-rate estimate by zero-padded FFT
/// (`08_ESTIMATOR_ENGINE.md §2`) and scores its spectral confidence (`08 §3`, spectral-SNR component).
///
/// The analysis window is zero-padded to at least 1,024 samples — a power of two, as Commons Math's
/// `FastFourierTransformer` requires — improving the bin resolution from ~7.5 bpm to ~1.76 bpm
/// (`08 §2`). The magnitude peak is searched only within the 0.7–2.5 Hz physiological band, re-checked
/// here even though the bandpass filter already suppresses out-of-band energy (`08 §2`).
///
/// **Confidence scope.** This computes only the spectral-SNR component of `08 §3`. The temporal-
/// consistency and ROI-stability components need session-level context (prior estimates, the ROI
/// stream) that a single `PpgWaveform` does not carry, so the full three-component score is composed
/// by the orchestrator at Phase 3 (T-302); see ADR 0004. Package-private collaborator of the
/// estimators; immutable and thread-safe.
final class PulseSpectrumAnalyzer {

    private static final double MIN_HZ = 0.7;
    private static final double MAX_HZ = 2.5;
    private static final double PEAK_HALF_WIDTH_HZ = 0.1;
    private static final int MIN_FFT_LENGTH = 1024;

    private final FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);

    /// Estimates the heart rate and spectral confidence from a filtered pulse signal.
    ///
    /// @param pulse the filtered, single-channel pulse signal; never `null`
    /// @param sampleRateHz the sampling rate of the pulse signal in hertz; strictly positive
    /// @param timestamp the instant to stamp on the estimate; never `null`
    /// @param algorithm the identifier of the producing estimator, for provenance and error context
    /// @return the heart-rate estimate carrying its spectral-SNR confidence
    /// @throws SignalQualityException if the analysis band holds no spectral energy (degenerate window)
    HeartRateEstimate estimate(double[] pulse, double sampleRateHz, Instant timestamp, String algorithm) {
        int paddedLength = paddedLength(pulse.length);
        double[] input = new double[paddedLength];
        applyHannWindow(pulse, input);
        Complex[] spectrum = transformer.transform(input, TransformType.FORWARD);

        double binHz = sampleRateHz / paddedLength;
        int lowBin = (int) Math.ceil(MIN_HZ / binHz);
        int highBin = (int) Math.floor(MAX_HZ / binHz);

        int peakBin = peakBin(spectrum, lowBin, highBin, algorithm);
        double bpm = peakBin * binHz * 60.0;
        double confidence = spectralConfidence(spectrum, binHz, lowBin, highBin, peakBin);
        return new HeartRateEstimate(bpm, confidence, timestamp, algorithm);
    }

    /// Copies the pulse into the (zero-padded) FFT buffer through a Hann window. Tapering the ends to
    /// zero removes the discontinuity at the zero-padding boundary, whose broadband leakage would
    /// otherwise dominate a small pulse near the passband edge; it is the same Hanning weighting
    /// `07 §9` uses for reconstruction.
    private static void applyHannWindow(double[] pulse, double[] destination) {
        int length = pulse.length;
        if (length < 2) {
            System.arraycopy(pulse, 0, destination, 0, length);
            return;
        }
        for (int i = 0; i < length; i++) {
            double weight = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (length - 1)));
            destination[i] = pulse[i] * weight;
        }
    }

    private static int paddedLength(int length) {
        int padded = MIN_FFT_LENGTH;
        while (padded < length) {
            padded <<= 1;
        }
        return padded;
    }

    private static int peakBin(Complex[] spectrum, int lowBin, int highBin, String algorithm) {
        int peakBin = -1;
        double peakMagnitude = 0.0;
        for (int k = lowBin; k <= highBin; k++) {
            double magnitude = spectrum[k].abs();
            if (magnitude > peakMagnitude) {
                peakMagnitude = magnitude;
                peakBin = k;
            }
        }
        if (peakBin < 0 || peakMagnitude <= 0.0) {
            throw new SignalQualityException(algorithm, "no spectral energy in the 0.7-2.5 Hz analysis band");
        }
        return peakBin;
    }

    private static double spectralConfidence(Complex[] spectrum, double binHz, int lowBin, int highBin, int peakBin) {
        double peakFrequency = peakBin * binHz;
        double peakPower = 0.0;
        double bandPower = 0.0;
        for (int k = lowBin; k <= highBin; k++) {
            double magnitude = spectrum[k].abs();
            double power = magnitude * magnitude;
            bandPower += power;
            if (Math.abs(k * binHz - peakFrequency) <= PEAK_HALF_WIDTH_HZ) {
                peakPower += power;
            }
        }
        double noisePower = bandPower - peakPower;
        if (noisePower <= 0.0) {
            return 1.0;
        }
        double snr = peakPower / noisePower;
        return snr / (1.0 + snr);
    }
}
