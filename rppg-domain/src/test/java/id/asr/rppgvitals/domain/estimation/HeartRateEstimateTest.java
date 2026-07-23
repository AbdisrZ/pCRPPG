package id.asr.rppgvitals.domain.estimation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class HeartRateEstimateTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:15:30Z");

    @Test
    void constructor_withValidArguments_retainsValues() {
        HeartRateEstimate estimate = new HeartRateEstimate(72.0, 0.85, NOW, "CHROM");

        assertEquals(72.0, estimate.beatsPerMinute());
        assertEquals(0.85, estimate.confidence());
        assertEquals(NOW, estimate.timestamp());
        assertEquals("CHROM", estimate.algorithm());
    }

    @Test
    void constructor_withBoundaryConfidences_isAccepted() {
        assertEquals(0.0, new HeartRateEstimate(0.0, 0.0, NOW, "POS").confidence());
        assertEquals(1.0, new HeartRateEstimate(150.0, 1.0, NOW, "POS").confidence());
    }

    @Test
    void constructor_withNullTimestamp_throws() {
        assertThrows(NullPointerException.class, () -> new HeartRateEstimate(72.0, 0.5, null, "CHROM"));
    }

    @Test
    void constructor_withNullAlgorithm_throws() {
        assertThrows(NullPointerException.class, () -> new HeartRateEstimate(72.0, 0.5, NOW, null));
    }

    @Test
    void constructor_withBlankAlgorithm_throws() {
        assertThrows(IllegalArgumentException.class, () -> new HeartRateEstimate(72.0, 0.5, NOW, " "));
    }

    @Test
    void constructor_withNegativeBpm_throws() {
        assertThrows(IllegalArgumentException.class, () -> new HeartRateEstimate(-1.0, 0.5, NOW, "CHROM"));
    }

    @Test
    void constructor_withNonFiniteBpm_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HeartRateEstimate(Double.POSITIVE_INFINITY, 0.5, NOW, "CHROM"));
    }

    @Test
    void constructor_withConfidenceBelowRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> new HeartRateEstimate(72.0, -0.01, NOW, "CHROM"));
    }

    @Test
    void constructor_withConfidenceAboveRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> new HeartRateEstimate(72.0, 1.01, NOW, "CHROM"));
    }
}
