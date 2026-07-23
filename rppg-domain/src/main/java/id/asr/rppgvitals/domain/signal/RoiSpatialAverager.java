package id.asr.rppgvitals.domain.signal;

import java.util.Objects;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;

/// Reduces a frame region to one rPPG sample by spatial averaging (`07_SIGNAL_PROCESSING.md §4`).
///
/// For the pixels inside the region of interest it computes the mean of each RGB channel, producing
/// the `[x_r, x_g, x_b]` triple the estimators consume. It reads the frame's canonical RGB layout
/// ([Frame]); the region is clamped to the frame bounds. Domain Service (`03_ARCHITECTURE.md §4`),
/// stateless and therefore thread-safe.
public final class RoiSpatialAverager {

    /// Creates the averager.
    public RoiSpatialAverager() {}

    /// Computes the per-channel spatial mean within the region of interest.
    ///
    /// @param frame the frame to sample; never `null`
    /// @param roi the region to average over; never `null`
    /// @return one sample carrying the frame's timestamp and the mean red, green, and blue values
    /// @throws IllegalArgumentException if the region does not overlap the frame at all
    public PpgSample sample(Frame frame, RegionOfInterest roi) {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(roi, "roi");
        int startX = Math.max(0, roi.x());
        int startY = Math.max(0, roi.y());
        int endX = Math.min(frame.width(), roi.x() + roi.width());
        int endY = Math.min(frame.height(), roi.y() + roi.height());
        if (endX <= startX || endY <= startY) {
            throw new IllegalArgumentException("region of interest does not overlap the frame");
        }

        byte[] pixels = frame.pixels();
        long sumRed = 0;
        long sumGreen = 0;
        long sumBlue = 0;
        for (int y = startY; y < endY; y++) {
            int rowBase = y * frame.width() * Frame.CHANNELS;
            for (int x = startX; x < endX; x++) {
                int index = rowBase + x * Frame.CHANNELS;
                sumRed += pixels[index] & 0xFF;
                sumGreen += pixels[index + 1] & 0xFF;
                sumBlue += pixels[index + 2] & 0xFF;
            }
        }
        long count = (long) (endX - startX) * (endY - startY);
        return new PpgSample(
                frame.capturedAt(), (double) sumRed / count, (double) sumGreen / count, (double) sumBlue / count);
    }
}
