package id.asr.rppgvitals.application.usecase.device;

import java.util.List;
import java.util.Objects;

import id.asr.rppgvitals.domain.capture.FrameSource;

/// Enumerates the camera devices available to start a session against (`03_ARCHITECTURE.md §6.2`,
/// `02_SOFTWARE_REQUIREMENT.md` FR-501, FR-502).
///
/// A discrete use case delegating to the [FrameSource] port so the presentation layer can offer a
/// device to select before capture begins.
public final class ListAvailableCameraDevicesUseCase {

    private final FrameSource frameSource;

    /// Creates the use case.
    ///
    /// @param frameSource the capture port to enumerate devices from; never `null`
    public ListAvailableCameraDevicesUseCase(FrameSource frameSource) {
        this.frameSource = Objects.requireNonNull(frameSource, "frameSource");
    }

    /// Returns the identifiers of the camera devices currently available.
    ///
    /// @return the available device identifiers; empty when no camera is present
    public List<String> execute() {
        return frameSource.availableDeviceIds();
    }
}
