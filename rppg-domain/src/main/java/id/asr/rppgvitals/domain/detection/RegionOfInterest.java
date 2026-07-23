package id.asr.rppgvitals.domain.detection;

/// A detected face region within a [id.asr.rppgvitals.domain.capture.Frame], expressed as an
/// axis-aligned bounding box together with the detector's confidence (`03_ARCHITECTURE.md §3`).
///
/// The bounding box delimits the pixels over which the rPPG spatial mean is computed
/// (`07_SIGNAL_PROCESSING.md §4`); `detectionConfidence` feeds the ROI-stability component of the
/// heart-rate confidence score (`08_ESTIMATOR_ENGINE.md §3`). How the box was derived from raw model
/// landmarks is the inference adapter's concern (`09_AI_INTEGRATION.md`), not the domain's.
///
/// @param x the left edge of the box in frame pixel coordinates; never negative
/// @param y the top edge of the box in frame pixel coordinates; never negative
/// @param width the box width in pixels; strictly positive
/// @param height the box height in pixels; strictly positive
/// @param detectionConfidence the detector's confidence in the region, in the closed range `[0, 1]`
public record RegionOfInterest(int x, int y, int width, int height, double detectionConfidence) {

    /// Validates the region geometry and confidence range.
    public RegionOfInterest {
        if (x < 0) {
            throw new IllegalArgumentException("x must not be negative, was " + x);
        }
        if (y < 0) {
            throw new IllegalArgumentException("y must not be negative, was " + y);
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive, was " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive, was " + height);
        }
        if (!(detectionConfidence >= 0.0 && detectionConfidence <= 1.0)) {
            throw new IllegalArgumentException("detectionConfidence must be in [0, 1], was " + detectionConfidence);
        }
    }

    /// Returns the number of pixels enclosed by the bounding box.
    ///
    /// @return the product of width and height
    public long area() {
        return (long) width * height;
    }
}
