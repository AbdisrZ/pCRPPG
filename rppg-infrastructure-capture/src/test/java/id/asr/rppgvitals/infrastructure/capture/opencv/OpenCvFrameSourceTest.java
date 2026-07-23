package id.asr.rppgvitals.infrastructure.capture.opencv;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.capture.CaptureConfiguration;
import id.asr.rppgvitals.domain.exception.CameraUnavailableException;

/// Exercises only the paths of [OpenCvFrameSource] that do not touch real camera hardware — the
/// device-id validation, the open-before-use guard, and the lifecycle guards. Actual capture from a
/// camera (and the device probe) is verified with hardware via the manual checklist (`13_TESTING.md §4`),
/// not here, so that this test never activates a webcam.
class OpenCvFrameSourceTest {

    @Test
    void open_withNonNumericDeviceId_throwsCameraUnavailable() {
        OpenCvFrameSource source = new OpenCvFrameSource();

        assertThrows(
                CameraUnavailableException.class,
                () -> source.open(new CaptureConfiguration("not-a-camera-index", 640, 480, 30)));
    }

    @Test
    void nextFrame_beforeOpen_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> new OpenCvFrameSource().nextFrame());
    }

    @Test
    void isDeviceAvailable_beforeOpen_isFalse() {
        assertFalse(new OpenCvFrameSource().isDeviceAvailable());
    }

    @Test
    void close_beforeOpen_isANoOp() {
        assertDoesNotThrow(() -> new OpenCvFrameSource().close());
    }
}
