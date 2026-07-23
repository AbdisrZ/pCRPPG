package id.asr.rppgvitals.domain.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CaptureConfigurationTest {

    @Test
    void constructor_withValidArguments_retainsValues() {
        CaptureConfiguration configuration = new CaptureConfiguration("cam-0", 640, 480, 30);

        assertEquals("cam-0", configuration.deviceId());
        assertEquals(640, configuration.frameWidth());
        assertEquals(480, configuration.frameHeight());
        assertEquals(30, configuration.targetFrameRate());
    }

    @Test
    void constructor_withNullDeviceId_throws() {
        assertThrows(NullPointerException.class, () -> new CaptureConfiguration(null, 640, 480, 30));
    }

    @Test
    void constructor_withBlankDeviceId_throws() {
        assertThrows(IllegalArgumentException.class, () -> new CaptureConfiguration("  ", 640, 480, 30));
    }

    @Test
    void constructor_withNonPositiveWidth_throws() {
        assertThrows(IllegalArgumentException.class, () -> new CaptureConfiguration("cam-0", 0, 480, 30));
    }

    @Test
    void constructor_withNonPositiveHeight_throws() {
        assertThrows(IllegalArgumentException.class, () -> new CaptureConfiguration("cam-0", 640, 0, 30));
    }

    @Test
    void constructor_withNonPositiveFrameRate_throws() {
        assertThrows(IllegalArgumentException.class, () -> new CaptureConfiguration("cam-0", 640, 480, 0));
    }
}
