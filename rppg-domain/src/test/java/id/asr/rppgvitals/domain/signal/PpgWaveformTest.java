package id.asr.rppgvitals.domain.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PpgWaveformTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:15:30Z");

    private static PpgSample sample() {
        return new PpgSample(NOW, 1.0, 1.0, 1.0);
    }

    @Test
    void constructor_withValidArguments_retainsValues() {
        PpgWaveform waveform = new PpgWaveform(List.of(sample(), sample()), 30.0);

        assertEquals(2, waveform.size());
        assertEquals(30.0, waveform.samplingRateHz());
    }

    @Test
    void durationSeconds_derivesFromSizeAndRate() {
        List<PpgSample> samples = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            samples.add(sample());
        }

        PpgWaveform waveform = new PpgWaveform(samples, 30.0);

        assertEquals(2.0, waveform.durationSeconds());
    }

    @Test
    void constructor_copiesSamplesDefensively() {
        List<PpgSample> mutable = new ArrayList<>();
        mutable.add(sample());

        PpgWaveform waveform = new PpgWaveform(mutable, 30.0);
        mutable.add(sample());

        assertEquals(1, waveform.size());
    }

    @Test
    void samples_returnsImmutableList() {
        PpgWaveform waveform = new PpgWaveform(List.of(sample()), 30.0);

        assertThrows(
                UnsupportedOperationException.class, () -> waveform.samples().add(sample()));
    }

    @Test
    void constructor_withNullSamples_throws() {
        assertThrows(NullPointerException.class, () -> new PpgWaveform(null, 30.0));
    }

    @Test
    void constructor_withEmptySamples_throws() {
        assertThrows(IllegalArgumentException.class, () -> new PpgWaveform(List.of(), 30.0));
    }

    @Test
    void constructor_withNonPositiveRate_throws() {
        assertThrows(IllegalArgumentException.class, () -> new PpgWaveform(List.of(sample()), 0.0));
    }

    @Test
    void constructor_withNonFiniteRate_throws() {
        assertThrows(IllegalArgumentException.class, () -> new PpgWaveform(List.of(sample()), Double.NaN));
    }
}
