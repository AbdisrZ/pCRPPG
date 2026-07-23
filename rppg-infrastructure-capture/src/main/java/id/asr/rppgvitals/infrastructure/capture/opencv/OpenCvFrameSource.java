package id.asr.rppgvitals.infrastructure.capture.opencv;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import id.asr.rppgvitals.domain.capture.CaptureConfiguration;
import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.capture.FrameSource;
import id.asr.rppgvitals.domain.exception.CameraUnavailableException;

/// OpenCV adapter implementing the domain [FrameSource] port (`03_ARCHITECTURE.md §6.3`).
///
/// It wraps OpenCV's `VideoCapture`, opening a camera by index (the device identifier is the camera
/// index as a string), yielding frames converted to the domain's canonical RGB layout via
/// [OpenCvFrames] (`ADR 0007`), and releasing the device on [#close()]. OpenCV error conditions are
/// translated into [CameraUnavailableException] at this boundary (`00_MASTER_PROMPT.md §22.1`); no
/// OpenCV type escapes inward.
///
/// **Thread-safety.** Not thread-safe: a single owning thread — the capture stage of the
/// `LiveMeasurementOrchestrator` (`11_THREADING.md`) — opens, polls, and closes it.
public final class OpenCvFrameSource implements FrameSource {

    private static final int MAX_PROBED_DEVICES = 5;

    static {
        OpenCV.loadLocally();
    }

    private VideoCapture capture;
    private String deviceId;
    private long sequenceIndex;

    /// Creates the frame source; the OpenCV native library is loaded on class initialisation.
    public OpenCvFrameSource() {}

    /// {@inheritDoc}
    @Override
    public List<String> availableDeviceIds() {
        List<String> ids = new ArrayList<>();
        for (int index = 0; index < MAX_PROBED_DEVICES; index++) {
            VideoCapture probe = new VideoCapture(index);
            if (probe.isOpened()) {
                ids.add(Integer.toString(index));
            }
            probe.release();
        }
        return List.copyOf(ids);
    }

    /// {@inheritDoc}
    @Override
    public void open(CaptureConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        int index = parseIndex(configuration.deviceId());
        VideoCapture opened = new VideoCapture(index);
        opened.set(Videoio.CAP_PROP_FRAME_WIDTH, configuration.frameWidth());
        opened.set(Videoio.CAP_PROP_FRAME_HEIGHT, configuration.frameHeight());
        opened.set(Videoio.CAP_PROP_FPS, configuration.targetFrameRate());
        if (!opened.isOpened()) {
            opened.release();
            throw new CameraUnavailableException(
                    configuration.deviceId(), "cannot open camera " + configuration.deviceId());
        }
        this.capture = opened;
        this.deviceId = configuration.deviceId();
        this.sequenceIndex = 0;
    }

    /// {@inheritDoc}
    @Override
    public Frame nextFrame() {
        VideoCapture active = requireOpen();
        Mat frame = new Mat();
        try {
            if (!active.read(frame) || frame.empty()) {
                throw new CameraUnavailableException(deviceId, "no frame available from camera " + deviceId);
            }
            return OpenCvFrames.toRgbFrame(frame, sequenceIndex++, Instant.now());
        } finally {
            frame.release();
        }
    }

    /// {@inheritDoc}
    @Override
    public boolean isDeviceAvailable() {
        return capture != null && capture.isOpened();
    }

    /// {@inheritDoc}
    @Override
    public void close() {
        if (capture != null) {
            capture.release();
            capture = null;
        }
    }

    private VideoCapture requireOpen() {
        if (capture == null) {
            throw new IllegalStateException("open(...) must be called before nextFrame()");
        }
        return capture;
    }

    private static int parseIndex(String deviceId) {
        try {
            return Integer.parseInt(deviceId);
        } catch (NumberFormatException notAnIndex) {
            throw new CameraUnavailableException(deviceId, "device id must be a camera index: " + deviceId, notAnIndex);
        }
    }
}
