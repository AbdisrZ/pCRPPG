package id.asr.rppgvitals.infrastructure.capture.opencv;

import java.time.Instant;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import id.asr.rppgvitals.domain.capture.Frame;

/// Converts an OpenCV capture `Mat` into the domain's canonical [Frame] (`ADR 0007`).
///
/// OpenCV delivers frames as 8-bit **BGR**; the domain expects 8-bit **RGB**, row-major
/// ([Frame]). This helper performs that colour-space conversion and extracts the pixel bytes, which is
/// the pure, testable part of the capture adapter (the `VideoCapture` I/O in [OpenCvFrameSource] is
/// not unit-testable without a camera). Package-private; assumes the OpenCV native library is loaded.
final class OpenCvFrames {

    private OpenCvFrames() {}

    /// Converts a BGR capture matrix into a canonical RGB [Frame].
    ///
    /// @param bgr an 8-bit 3-channel BGR matrix from OpenCV; never `null`
    /// @param sequenceIndex the capture index to stamp on the frame
    /// @param capturedAt the capture instant; never `null`
    /// @return the frame in the domain's canonical RGB layout
    static Frame toRgbFrame(Mat bgr, long sequenceIndex, Instant capturedAt) {
        Mat rgb = new Mat();
        try {
            Imgproc.cvtColor(bgr, rgb, Imgproc.COLOR_BGR2RGB);
            int width = rgb.cols();
            int height = rgb.rows();
            byte[] pixels = new byte[width * height * Frame.CHANNELS];
            int read = rgb.get(0, 0, pixels);
            if (read != pixels.length) {
                throw new IllegalStateException("expected " + pixels.length + " pixel bytes, read " + read);
            }
            return new Frame(pixels, width, height, sequenceIndex, capturedAt);
        } finally {
            rgb.release();
        }
    }
}
