package id.asr.rppgvitals.domain.capture;

import java.util.Objects;

/// The selected camera device and the target capture parameters for a measurement session
/// (`03_ARCHITECTURE.md §3`, tracing `02_SOFTWARE_REQUIREMENT.md` FR-501 and FR-502).
///
/// A `FrameSource` is opened against a `CaptureConfiguration`; the values here are *requests* to the
/// capture backend, which may deliver the nearest supported mode.
///
/// @param deviceId the identifier of the selected camera device; never `null` or blank
/// @param frameWidth the requested frame width in pixels; strictly positive
/// @param frameHeight the requested frame height in pixels; strictly positive
/// @param targetFrameRate the requested capture rate in frames per second; strictly positive
public record CaptureConfiguration(String deviceId, int frameWidth, int frameHeight, int targetFrameRate) {

    /// Validates the configuration.
    public CaptureConfiguration {
        Objects.requireNonNull(deviceId, "deviceId");
        if (deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be blank");
        }
        if (frameWidth <= 0) {
            throw new IllegalArgumentException("frameWidth must be positive, was " + frameWidth);
        }
        if (frameHeight <= 0) {
            throw new IllegalArgumentException("frameHeight must be positive, was " + frameHeight);
        }
        if (targetFrameRate <= 0) {
            throw new IllegalArgumentException("targetFrameRate must be positive, was " + targetFrameRate);
        }
    }
}
