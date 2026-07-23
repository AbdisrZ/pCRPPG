package id.asr.rppgvitals.domain.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;

class RoiSpatialAveragerTest {

    private static final Instant NOW = Instant.parse("2026-07-23T09:00:00Z");
    private final RoiSpatialAverager averager = new RoiSpatialAverager();

    /// Builds a `width`x`height` frame where every pixel has the same RGB triple.
    private static Frame uniformFrame(int width, int height, int red, int green, int blue) {
        byte[] pixels = new byte[width * height * Frame.CHANNELS];
        for (int i = 0; i < width * height; i++) {
            pixels[i * Frame.CHANNELS] = (byte) red;
            pixels[i * Frame.CHANNELS + 1] = (byte) green;
            pixels[i * Frame.CHANNELS + 2] = (byte) blue;
        }
        return new Frame(pixels, width, height, 0L, NOW);
    }

    @Test
    void sample_overUniformRegion_returnsThatColourAndTimestamp() {
        Frame frame = uniformFrame(4, 4, 10, 20, 30);

        PpgSample sample = averager.sample(frame, new RegionOfInterest(0, 0, 4, 4, 0.9));

        assertEquals(NOW, sample.timestamp());
        assertEquals(10.0, sample.red());
        assertEquals(20.0, sample.green());
        assertEquals(30.0, sample.blue());
    }

    @Test
    void sample_readsHighByteValuesUnsigned() {
        Frame frame = uniformFrame(2, 2, 200, 0, 255);

        PpgSample sample = averager.sample(frame, new RegionOfInterest(0, 0, 2, 2, 1.0));

        assertEquals(200.0, sample.red());
        assertEquals(255.0, sample.blue());
    }

    @Test
    void sample_averagesOnlyTheRegion() {
        // Left column green=100, right column green=0; a region over the left column averages to 100.
        byte[] pixels = new byte[2 * 1 * Frame.CHANNELS];
        pixels[1] = (byte) 100; // pixel (0,0) green
        Frame frame = new Frame(pixels, 2, 1, 0L, NOW);

        PpgSample sample = averager.sample(frame, new RegionOfInterest(0, 0, 1, 1, 1.0));

        assertEquals(100.0, sample.green());
    }

    @Test
    void sample_clampsRegionToFrameBounds() {
        Frame frame = uniformFrame(2, 2, 40, 40, 40);

        PpgSample sample = averager.sample(frame, new RegionOfInterest(1, 1, 10, 10, 1.0));

        assertEquals(40.0, sample.red());
    }

    @Test
    void sample_withNonOverlappingRegion_throws() {
        Frame frame = uniformFrame(2, 2, 10, 10, 10);

        assertThrows(
                IllegalArgumentException.class, () -> averager.sample(frame, new RegionOfInterest(5, 5, 2, 2, 1.0)));
    }

    @Test
    void sample_withNullArguments_throws() {
        Frame frame = uniformFrame(2, 2, 10, 10, 10);
        RegionOfInterest roi = new RegionOfInterest(0, 0, 2, 2, 1.0);

        assertThrows(NullPointerException.class, () -> averager.sample(null, roi));
        assertThrows(NullPointerException.class, () -> averager.sample(frame, null));
    }
}
