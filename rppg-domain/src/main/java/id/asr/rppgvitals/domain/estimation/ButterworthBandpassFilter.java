package id.asr.rppgvitals.domain.estimation;

import org.apache.commons.math3.complex.Complex;

/// A zero-phase Butterworth bandpass filter (`07_SIGNAL_PROCESSING.md §9`).
///
/// The digital transfer function is designed at construction from an analog Butterworth low-pass
/// prototype via the low-pass-to-bandpass transform and the bilinear transform, the standard design
/// path (the same one `scipy.signal.butter` follows). Filtering is applied forward then backward
/// ("filtfilt") so the net phase response is zero, as `07 §9` requires — a plain single pass would
/// shift the pulse peaks in time.
///
/// Package-private: an internal collaborator of the `SignalEstimator` implementations, not part of
/// the domain's public API. Instances are immutable and stateless across calls, hence thread-safe.
final class ButterworthBandpassFilter {

    private final double[] feedforward;
    private final double[] feedback;

    /// Designs the filter for the given order and passband.
    ///
    /// @param order the Butterworth order per band edge (3 per `07 §9`); strictly positive
    /// @param lowHz the lower passband edge in hertz; strictly positive and below `highHz`
    /// @param highHz the upper passband edge in hertz; below the Nyquist frequency
    /// @param sampleRateHz the sampling rate in hertz; strictly positive
    ButterworthBandpassFilter(int order, double lowHz, double highHz, double sampleRateHz) {
        if (order <= 0) {
            throw new IllegalArgumentException("order must be positive, was " + order);
        }
        if (!(lowHz > 0.0) || !(highHz > lowHz) || !(highHz < sampleRateHz / 2.0)) {
            throw new IllegalArgumentException("require 0 < lowHz < highHz < Nyquist");
        }
        double[][] coefficients = design(order, lowHz, highHz, sampleRateHz);
        this.feedforward = coefficients[0];
        this.feedback = coefficients[1];
    }

    /// Applies the filter with zero net phase (forward then reverse pass).
    ///
    /// The signal is extended at both ends by odd (point-symmetric) reflection before filtering and
    /// trimmed back afterward, the standard filtfilt edge handling (as in `scipy.signal.filtfilt`):
    /// without it, the startup transient of a finite signal injects spurious low-frequency energy
    /// near the passband edge.
    ///
    /// @param signal the input samples; never `null`
    /// @return a new array of the same length holding the filtered signal
    double[] filtfilt(double[] signal) {
        int padLength = Math.min(3 * (Math.max(feedforward.length, feedback.length) - 1), signal.length - 1);
        double[] padded = oddReflectionPad(signal, padLength);

        double[] forward = applyOnce(padded);
        reverse(forward);
        double[] backward = applyOnce(forward);
        reverse(backward);

        double[] result = new double[signal.length];
        System.arraycopy(backward, padLength, result, 0, signal.length);
        return result;
    }

    private static double[] oddReflectionPad(double[] x, int padLength) {
        int n = x.length;
        double[] padded = new double[n + 2 * padLength];
        for (int k = 0; k < padLength; k++) {
            padded[k] = 2.0 * x[0] - x[padLength - k];
            padded[padLength + n + k] = 2.0 * x[n - 1] - x[n - 2 - k];
        }
        System.arraycopy(x, 0, padded, padLength, n);
        return padded;
    }

    private double[] applyOnce(double[] x) {
        int n = feedback.length;
        double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            double acc = 0.0;
            for (int j = 0; j < feedforward.length; j++) {
                if (i - j >= 0) {
                    acc += feedforward[j] * x[i - j];
                }
            }
            for (int j = 1; j < n; j++) {
                if (i - j >= 0) {
                    acc -= feedback[j] * y[i - j];
                }
            }
            y[i] = acc / feedback[0];
        }
        return y;
    }

    private static void reverse(double[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            double tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    private static double[][] design(int order, double lowHz, double highHz, double fs) {
        double warpedLow = 2.0 * fs * Math.tan(Math.PI * lowHz / fs);
        double warpedHigh = 2.0 * fs * Math.tan(Math.PI * highHz / fs);
        double bandwidth = warpedHigh - warpedLow;
        double centerSquared = warpedLow * warpedHigh;

        Complex[] analogPoles = bandpassPoles(prototypePoles(order), bandwidth, centerSquared);
        double analogGain = Math.pow(bandwidth, order);

        return bilinear(analogPoles, order, analogGain, fs);
    }

    private static Complex[] prototypePoles(int order) {
        Complex[] poles = new Complex[order];
        for (int k = 0; k < order; k++) {
            double theta = Math.PI * (2.0 * k + order + 1.0) / (2.0 * order);
            poles[k] = new Complex(Math.cos(theta), Math.sin(theta));
        }
        return poles;
    }

    private static Complex[] bandpassPoles(Complex[] prototype, double bandwidth, double centerSquared) {
        Complex[] mapped = new Complex[prototype.length * 2];
        for (int i = 0; i < prototype.length; i++) {
            Complex half = prototype[i].multiply(bandwidth / 2.0);
            Complex root = half.multiply(half).subtract(centerSquared).sqrt();
            mapped[2 * i] = half.add(root);
            mapped[2 * i + 1] = half.subtract(root);
        }
        return mapped;
    }

    private static double[][] bilinear(Complex[] analogPoles, int order, double analogGain, double fs) {
        double twoFs = 2.0 * fs;
        Complex[] digitalPoles = new Complex[analogPoles.length];
        Complex gainDenominator = Complex.ONE;
        for (int i = 0; i < analogPoles.length; i++) {
            digitalPoles[i] =
                    new Complex(twoFs).add(analogPoles[i]).divide(new Complex(twoFs).subtract(analogPoles[i]));
            gainDenominator = gainDenominator.multiply(new Complex(twoFs).subtract(analogPoles[i]));
        }
        // Analog bandpass has `order` zeros at s = 0, which map to z = -1 under the bilinear transform;
        // the remaining (order) zeros are added at z = -1 for the pole/zero-count difference.
        Complex[] digitalZeros = new Complex[analogPoles.length];
        Complex minusOne = new Complex(-1.0);
        for (int i = 0; i < order; i++) {
            digitalZeros[i] = Complex.ONE;
            digitalZeros[i + order] = minusOne;
        }
        Complex numeratorProduct = Complex.ONE;
        for (int i = 0; i < order; i++) {
            numeratorProduct = numeratorProduct.multiply(new Complex(twoFs));
        }
        double gain = analogGain * numeratorProduct.divide(gainDenominator).getReal();

        double[] b = realPolynomialFromRoots(digitalZeros);
        double[] a = realPolynomialFromRoots(digitalPoles);
        for (int i = 0; i < b.length; i++) {
            b[i] *= gain;
        }
        return new double[][] {b, a};
    }

    private static double[] realPolynomialFromRoots(Complex[] roots) {
        Complex[] coefficients = new Complex[] {Complex.ONE};
        for (Complex root : roots) {
            Complex[] next = new Complex[coefficients.length + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = Complex.ZERO;
            }
            for (int i = 0; i < coefficients.length; i++) {
                next[i] = next[i].add(coefficients[i]);
                next[i + 1] = next[i + 1].subtract(coefficients[i].multiply(root));
            }
            coefficients = next;
        }
        double[] real = new double[coefficients.length];
        for (int i = 0; i < coefficients.length; i++) {
            real[i] = coefficients[i].getReal();
        }
        return real;
    }
}
