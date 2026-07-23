package id.asr.rppgvitals.domain.exception;

/// Signals that a camera device could not be opened or became unavailable mid-session
/// (`00_MASTER_PROMPT.md §22.2`). Thrown by the capture adapter and translated by the orchestrator
/// into a [id.asr.rppgvitals.domain.signal.SignalQuality.Degraded] state (`03_ARCHITECTURE.md §7.2`).
///
/// Carries the identifier of the device involved so the failure is actionable.
public final class CameraUnavailableException extends RppgApplicationException {

    private static final long serialVersionUID = 1L;

    private final String deviceId;

    /// Creates the exception for a specific device.
    ///
    /// @param deviceId the identifier of the camera device that is unavailable; never `null`
    /// @param message the actionable failure description; never `null`
    public CameraUnavailableException(String deviceId, String message) {
        super(message);
        this.deviceId = deviceId;
    }

    /// Creates the exception for a specific device, wrapping an underlying cause.
    ///
    /// @param deviceId the identifier of the camera device that is unavailable; never `null`
    /// @param message the actionable failure description; never `null`
    /// @param cause the underlying capture-backend failure being translated; may be `null`
    public CameraUnavailableException(String deviceId, String message, Throwable cause) {
        super(message, cause);
        this.deviceId = deviceId;
    }

    /// Returns the identifier of the camera device that is unavailable.
    ///
    /// @return the device identifier
    public String deviceId() {
        return deviceId;
    }
}
