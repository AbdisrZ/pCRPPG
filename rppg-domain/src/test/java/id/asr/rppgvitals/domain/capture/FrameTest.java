package id.asr.rppgvitals.domain.capture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class FrameTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:15:30Z");

    @Test
    void constructor_withValidArguments_retainsValues() {
        byte[] pixels = new byte[2 * 2 * Frame.CHANNELS];
        pixels[0] = 5;

        Frame frame = new Frame(pixels, 2, 2, 7L, NOW);

        assertArrayEquals(pixels, frame.pixels());
        assertEquals(2, frame.width());
        assertEquals(2, frame.height());
        assertEquals(7L, frame.sequenceIndex());
        assertEquals(NOW, frame.capturedAt());
    }

    @Test
    void pixelCount_withDimensions_returnsProduct() {
        Frame frame = new Frame(new byte[3 * 2 * Frame.CHANNELS], 3, 2, 0L, NOW);

        assertEquals(6L, frame.pixelCount());
    }

    @Test
    void constructor_copiesPixelsDefensively() {
        byte[] source = {1, 2, 3};
        Frame frame = new Frame(source, 1, 1, 0L, NOW);

        source[0] = 99;

        assertEquals(1, frame.pixels()[0]);
    }

    @Test
    void pixels_returnsDefensiveCopy() {
        Frame frame = new Frame(new byte[] {1, 2, 3}, 1, 1, 0L, NOW);

        frame.pixels()[0] = 99;

        assertEquals(1, frame.pixels()[0]);
    }

    @Test
    void constructor_withNullPixels_throws() {
        assertThrows(NullPointerException.class, () -> new Frame(null, 1, 1, 0L, NOW));
    }

    @Test
    void constructor_withNullTimestamp_throws() {
        assertThrows(NullPointerException.class, () -> new Frame(new byte[3], 1, 1, 0L, null));
    }

    @Test
    void constructor_withNonPositiveWidth_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Frame(new byte[3], 0, 1, 0L, NOW));
    }

    @Test
    void constructor_withNonPositiveHeight_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Frame(new byte[3], 1, 0, 0L, NOW));
    }

    @Test
    void constructor_withNegativeSequenceIndex_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Frame(new byte[3], 1, 1, -1L, NOW));
    }

    @Test
    void constructor_withPixelLengthNotMatchingDimensions_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Frame(new byte[5], 2, 2, 0L, NOW));
    }
}
