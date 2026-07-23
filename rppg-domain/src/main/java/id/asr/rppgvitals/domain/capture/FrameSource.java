package id.asr.rppgvitals.domain.capture;

import java.util.List;

import id.asr.rppgvitals.domain.exception.CameraUnavailableException;

/// Domain port for camera frame acquisition (`03_ARCHITECTURE.md §4`).
///
/// A `FrameSource` enumerates the available camera devices, opens a selected one against a
/// [CaptureConfiguration], yields a continuous stream of [Frame]s, and releases the device
/// deterministically on [#close()]. It is implemented in the infrastructure layer by
/// `OpenCvFrameSource`; the domain and application layers depend only on this contract.
///
/// **Thread-safety.** A `FrameSource` is a stateful, device-bound resource. Implementations are not
/// required to be thread-safe: a single owning thread (the capture stage of the
/// `LiveMeasurementOrchestrator`, per `11_THREADING.md`) opens, polls, and closes it.
public interface FrameSource extends AutoCloseable {

    /// Enumerates the identifiers of the camera devices currently available to open.
    ///
    /// @return an immutable list of device identifiers, possibly empty when no camera is present
    List<String> availableDeviceIds();

    /// Opens the device selected by the given configuration, making the source ready to yield frames.
    ///
    /// @param configuration the device selection and target capture parameters; never `null`
    /// @throws CameraUnavailableException if the selected device cannot be opened
    void open(CaptureConfiguration configuration);

    /// Returns the next captured frame, blocking until one is available.
    ///
    /// @return the next frame in capture order
    /// @throws CameraUnavailableException if the device becomes unavailable mid-stream
    Frame nextFrame();

    /// Reports whether the currently selected device is available, used for reconnect polling after a
    /// [CameraUnavailableException] (`03 §7.2`).
    ///
    /// @return `true` if the device is present and can yield frames, `false` otherwise
    boolean isDeviceAvailable();

    /// Releases the underlying device and any native resources. Idempotent.
    @Override
    void close();
}
