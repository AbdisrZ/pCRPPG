package id.asr.rppgvitals.infrastructure.capture.opencv;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import id.asr.rppgvitals.domain.capture.Frame;

class OpenCvFramesTest {

    private static final Instant NOW = Instant.parse("2026-07-24T09:00:00Z");

    @BeforeAll
    static void loadOpenCv() {
        OpenCV.loadLocally();
    }

    @Test
    void toRgbFrame_reordersBgrPixelsToRgb() {
        Mat bgr = new Mat(1, 1, CvType.CV_8UC3);
        bgr.put(0, 0, new byte[] {10, 20, 30}); // OpenCV order: B=10, G=20, R=30

        Frame frame = OpenCvFrames.toRgbFrame(bgr, 7L, NOW);
        bgr.release();

        assertArrayEquals(new byte[] {30, 20, 10}, frame.pixels()); // canonical RGB
        assertEquals(1, frame.width());
        assertEquals(1, frame.height());
        assertEquals(7L, frame.sequenceIndex());
        assertEquals(NOW, frame.capturedAt());
    }

    @Test
    void toRgbFrame_preservesDimensionsForAMultiPixelMatrix() {
        Mat bgr = new Mat(2, 3, CvType.CV_8UC3);
        bgr.setTo(new Scalar(10, 20, 30));

        Frame frame = OpenCvFrames.toRgbFrame(bgr, 0L, NOW);
        bgr.release();

        assertEquals(3, frame.width());
        assertEquals(2, frame.height());
        assertEquals(2 * 3 * Frame.CHANNELS, frame.pixels().length);
        assertEquals(30, frame.pixels()[0]); // R of the first pixel
    }
}
