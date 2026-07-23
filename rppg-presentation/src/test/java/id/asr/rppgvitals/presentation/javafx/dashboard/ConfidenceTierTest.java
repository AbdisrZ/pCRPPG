package id.asr.rppgvitals.presentation.javafx.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ConfidenceTierTest {

    @Test
    void fromConfidence_mapsScoresToTiersAtTheTierBoundaries() {
        assertEquals(ConfidenceTier.HIGH, ConfidenceTier.fromConfidence(1.0));
        assertEquals(ConfidenceTier.HIGH, ConfidenceTier.fromConfidence(0.8));
        assertEquals(ConfidenceTier.MODERATE, ConfidenceTier.fromConfidence(0.79));
        assertEquals(ConfidenceTier.MODERATE, ConfidenceTier.fromConfidence(0.5));
        assertEquals(ConfidenceTier.LOW, ConfidenceTier.fromConfidence(0.49));
        assertEquals(ConfidenceTier.LOW, ConfidenceTier.fromConfidence(0.0));
    }

    @Test
    void eachTier_carriesAColourAndAGuidanceMessage() {
        for (ConfidenceTier tier : ConfidenceTier.values()) {
            assertFalse(tier.colorHex().isBlank(), tier + " colour");
            assertFalse(tier.message().isBlank(), tier + " message");
        }
    }
}
