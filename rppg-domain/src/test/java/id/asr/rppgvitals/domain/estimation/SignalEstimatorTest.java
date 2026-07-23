package id.asr.rppgvitals.domain.estimation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import id.asr.rppgvitals.domain.exception.SignalQualityException;
import id.asr.rppgvitals.domain.signal.PpgSample;
import id.asr.rppgvitals.domain.signal.PpgWaveform;

class SignalEstimatorTest {

    private static final double FS = 30.0;
    private static final int SAMPLES = 300;
    private static final double PULSE_HZ = 1.2;
    private static final double EXPECTED_BPM = PULSE_HZ * 60.0;
    private static final Instant START = Instant.parse("2026-07-22T10:00:00Z");

    static Stream<SignalEstimator> estimators() {
        return Stream.of(new GreenChannelSignalEstimator(), new ChromSignalEstimator(), new PosSignalEstimator());
    }

    /// A synthetic window carrying a clean pulse at {@link #PULSE_HZ}, with the green channel
    /// modulated most strongly, matching the skin optical model the algorithms assume.
    private static PpgWaveform cleanPulse() {
        List<PpgSample> samples = new ArrayList<>(SAMPLES);
        for (int i = 0; i < SAMPLES; i++) {
            double pulse = Math.sin(2.0 * Math.PI * PULSE_HZ * i / FS);
            Instant timestamp = START.plusMillis(Math.round(1000.0 * i / FS));
            samples.add(new PpgSample(timestamp, 100.0 + 0.3 * pulse, 100.0 + 1.0 * pulse, 100.0 + 0.1 * pulse));
        }
        return new PpgWaveform(samples, FS);
    }

    private static PpgWaveform constant(double red, double green, double blue) {
        List<PpgSample> samples = new ArrayList<>(SAMPLES);
        for (int i = 0; i < SAMPLES; i++) {
            samples.add(new PpgSample(START.plusMillis(Math.round(1000.0 * i / FS)), red, green, blue));
        }
        return new PpgWaveform(samples, FS);
    }

    @ParameterizedTest
    @MethodSource("estimators")
    void estimate_withCleanPulse_recoversHeartRate(SignalEstimator estimator) {
        HeartRateEstimate estimate = estimator.estimate(cleanPulse());

        assertTrue(
                Math.abs(estimate.beatsPerMinute() - EXPECTED_BPM) < 2.5,
                estimator.getClass().getSimpleName() + " recovered " + estimate.beatsPerMinute() + " bpm");
    }

    @ParameterizedTest
    @MethodSource("estimators")
    void estimate_withCleanPulse_reportsHighConfidence(SignalEstimator estimator) {
        HeartRateEstimate estimate = estimator.estimate(cleanPulse());

        assertTrue(estimate.confidence() > 0.6, "confidence was " + estimate.confidence());
    }

    @ParameterizedTest
    @MethodSource("estimators")
    void estimate_withNullWaveform_throws(SignalEstimator estimator) {
        assertThrows(NullPointerException.class, () -> estimator.estimate(null));
    }

    @ParameterizedTest
    @MethodSource("estimators")
    void estimate_withZeroVarianceWindow_throwsSignalQuality(SignalEstimator estimator) {
        assertThrows(SignalQualityException.class, () -> estimator.estimate(constant(100.0, 100.0, 100.0)));
    }

    @Test
    void estimate_withZeroMeanChannel_throwsSignalQuality() {
        // A green channel that is identically zero has a zero window mean, so normalisation fails.
        assertThrows(
                SignalQualityException.class,
                () -> new GreenChannelSignalEstimator().estimate(constant(100.0, 0.0, 100.0)));
    }

    @Test
    void estimate_stampsTheLatestSampleTimestamp() {
        HeartRateEstimate estimate = new ChromSignalEstimator().estimate(cleanPulse());

        Instant expected = START.plusMillis(Math.round(1000.0 * (SAMPLES - 1) / FS));
        assertTrue(estimate.timestamp().equals(expected), "timestamp was " + estimate.timestamp());
    }
}
