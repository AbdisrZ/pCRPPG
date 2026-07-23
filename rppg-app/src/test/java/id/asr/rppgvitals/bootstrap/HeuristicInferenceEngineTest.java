package id.asr.rppgvitals.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;

class HeuristicInferenceEngineTest {

    private final HeuristicInferenceEngine engine = new HeuristicInferenceEngine();

    @Test
    void detectRegionOfInterest_returnsACentredHalfSizeRegion() {
        Frame frame = new Frame(new byte[40 * 20 * Frame.CHANNELS], 40, 20, 0L, Instant.EPOCH);

        RegionOfInterest roi = engine.detectRegionOfInterest(frame).orElseThrow();

        assertEquals(20, roi.width());
        assertEquals(10, roi.height());
        assertEquals(10, roi.x());
        assertEquals(5, roi.y());
        assertTrue(roi.detectionConfidence() > 0.0);
    }

    @Test
    void detectRegionOfInterest_withNullFrame_throws() {
        assertThrows(NullPointerException.class, () -> engine.detectRegionOfInterest(null));
    }
}
