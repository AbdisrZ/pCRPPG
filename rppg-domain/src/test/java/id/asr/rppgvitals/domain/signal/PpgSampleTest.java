package id.asr.rppgvitals.domain.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PpgSampleTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:15:30Z");

    @Test
    void constructor_withValidArguments_retainsValues() {
        PpgSample sample = new PpgSample(NOW, 100.5, 120.25, 90.0);

        assertEquals(NOW, sample.timestamp());
        assertEquals(100.5, sample.red());
        assertEquals(120.25, sample.green());
        assertEquals(90.0, sample.blue());
    }

    @Test
    void constructor_withNullTimestamp_throws() {
        assertThrows(NullPointerException.class, () -> new PpgSample(null, 1, 1, 1));
    }

    @Test
    void constructor_withNonFiniteRed_throws() {
        assertThrows(IllegalArgumentException.class, () -> new PpgSample(NOW, Double.NaN, 1, 1));
    }

    @Test
    void constructor_withNonFiniteGreen_throws() {
        assertThrows(IllegalArgumentException.class, () -> new PpgSample(NOW, 1, Double.POSITIVE_INFINITY, 1));
    }

    @Test
    void constructor_withNonFiniteBlue_throws() {
        assertThrows(IllegalArgumentException.class, () -> new PpgSample(NOW, 1, 1, Double.NEGATIVE_INFINITY));
    }
}
