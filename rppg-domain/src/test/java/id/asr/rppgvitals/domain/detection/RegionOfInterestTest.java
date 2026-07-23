package id.asr.rppgvitals.domain.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RegionOfInterestTest {

    @Test
    void constructor_withValidArguments_retainsValues() {
        RegionOfInterest roi = new RegionOfInterest(10, 20, 100, 80, 0.9);

        assertEquals(10, roi.x());
        assertEquals(20, roi.y());
        assertEquals(100, roi.width());
        assertEquals(80, roi.height());
        assertEquals(0.9, roi.detectionConfidence());
    }

    @Test
    void area_withDimensions_returnsProduct() {
        RegionOfInterest roi = new RegionOfInterest(0, 0, 100, 80, 1.0);

        assertEquals(8000L, roi.area());
    }

    @Test
    void constructor_withBoundaryConfidences_isAccepted() {
        assertEquals(0.0, new RegionOfInterest(0, 0, 1, 1, 0.0).detectionConfidence());
        assertEquals(1.0, new RegionOfInterest(0, 0, 1, 1, 1.0).detectionConfidence());
    }

    @Test
    void constructor_withNegativeX_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RegionOfInterest(-1, 0, 1, 1, 0.5));
    }

    @Test
    void constructor_withNegativeY_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RegionOfInterest(0, -1, 1, 1, 0.5));
    }

    @Test
    void constructor_withNonPositiveWidth_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RegionOfInterest(0, 0, 0, 1, 0.5));
    }

    @Test
    void constructor_withNonPositiveHeight_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RegionOfInterest(0, 0, 1, 0, 0.5));
    }

    @Test
    void constructor_withConfidenceBelowRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RegionOfInterest(0, 0, 1, 1, -0.01));
    }

    @Test
    void constructor_withConfidenceAboveRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RegionOfInterest(0, 0, 1, 1, 1.01));
    }
}
