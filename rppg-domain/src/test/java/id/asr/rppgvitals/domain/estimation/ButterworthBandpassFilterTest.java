package id.asr.rppgvitals.domain.estimation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ButterworthBandpassFilterTest {

    private static final double FS = 30.0;
    private static final int ORDER = 3;
    private static final double LOW_HZ = 0.7;
    private static final double HIGH_HZ = 2.5;
    private static final int LENGTH = 900;

    private static double[] sine(double freqHz) {
        double[] signal = new double[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            signal[i] = Math.sin(2.0 * Math.PI * freqHz * i / FS);
        }
        return signal;
    }

    private static double steadyStateAmplitude(double[] signal) {
        double max = 0.0;
        for (int i = LENGTH / 3; i < 2 * LENGTH / 3; i++) {
            max = Math.max(max, Math.abs(signal[i]));
        }
        return max;
    }

    @Test
    void filtfilt_passesFrequencyInsideTheBand() {
        ButterworthBandpassFilter filter = new ButterworthBandpassFilter(ORDER, LOW_HZ, HIGH_HZ, FS);

        double amplitude = steadyStateAmplitude(filter.filtfilt(sine(1.2)));

        assertTrue(amplitude > 0.85, "in-band amplitude should be near unity, was " + amplitude);
        assertTrue(amplitude < 1.15, "in-band amplitude should be near unity, was " + amplitude);
    }

    @Test
    void filtfilt_attenuatesFrequencyBelowTheBand() {
        ButterworthBandpassFilter filter = new ButterworthBandpassFilter(ORDER, LOW_HZ, HIGH_HZ, FS);

        double amplitude = steadyStateAmplitude(filter.filtfilt(sine(0.1)));

        assertTrue(amplitude < 0.1, "far below-band amplitude should be strongly attenuated, was " + amplitude);
    }

    @Test
    void filtfilt_attenuatesFrequencyAboveTheBand() {
        ButterworthBandpassFilter filter = new ButterworthBandpassFilter(ORDER, LOW_HZ, HIGH_HZ, FS);

        double amplitude = steadyStateAmplitude(filter.filtfilt(sine(6.0)));

        assertTrue(amplitude < 0.1, "far above-band amplitude should be strongly attenuated, was " + amplitude);
    }

    @Test
    void filtfilt_isZeroPhaseForAnInBandSine() {
        ButterworthBandpassFilter filter = new ButterworthBandpassFilter(ORDER, LOW_HZ, HIGH_HZ, FS);
        double[] input = sine(1.2);

        double[] output = filter.filtfilt(input);

        // Zero-phase filtering leaves peaks aligned in time: input and output correlate positively
        // with no lag. A single-pass filter would shift the peaks and lower this correlation.
        double dot = 0.0;
        double energy = 0.0;
        for (int i = LENGTH / 3; i < 2 * LENGTH / 3; i++) {
            dot += input[i] * output[i];
            energy += input[i] * input[i];
        }
        assertTrue(dot / energy > 0.85, "output should stay in phase with input, ratio was " + (dot / energy));
    }

    @Test
    void constructor_withInvalidBand_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ButterworthBandpassFilter(0, LOW_HZ, HIGH_HZ, FS));
        assertThrows(IllegalArgumentException.class, () -> new ButterworthBandpassFilter(ORDER, 2.5, 0.7, FS));
        assertThrows(IllegalArgumentException.class, () -> new ButterworthBandpassFilter(ORDER, 0.7, 20.0, FS));
    }
}
