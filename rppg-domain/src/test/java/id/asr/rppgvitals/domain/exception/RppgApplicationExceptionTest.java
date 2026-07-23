package id.asr.rppgvitals.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class RppgApplicationExceptionTest {

    private static final Throwable CAUSE = new IllegalStateException("root");

    @Test
    void allSubtypes_areUncheckedAndShareTheSealedRoot() {
        assertInstanceOf(RuntimeException.class, new CameraUnavailableException("cam-0", "m"));
        assertInstanceOf(RppgApplicationException.class, new SignalQualityException("CHROM", "m"));
        assertInstanceOf(RppgApplicationException.class, new ModelInferenceException("model", "m"));
        assertInstanceOf(RppgApplicationException.class, new PersistenceException("save", "m"));
        assertInstanceOf(RppgApplicationException.class, new ConfigurationException("key", "m"));
    }

    @Test
    void cameraUnavailable_carriesDeviceMessageAndCause() {
        CameraUnavailableException withoutCause = new CameraUnavailableException("cam-0", "unavailable");
        CameraUnavailableException withCause = new CameraUnavailableException("cam-1", "unavailable", CAUSE);

        assertEquals("cam-0", withoutCause.deviceId());
        assertEquals("unavailable", withoutCause.getMessage());
        assertEquals("cam-1", withCause.deviceId());
        assertSame(CAUSE, withCause.getCause());
    }

    @Test
    void signalQuality_carriesAlgorithmMessageAndCause() {
        SignalQualityException withoutCause = new SignalQualityException("POS", "degenerate window");
        SignalQualityException withCause = new SignalQualityException("POS", "degenerate window", CAUSE);

        assertEquals("POS", withoutCause.algorithm());
        assertEquals("degenerate window", withoutCause.getMessage());
        assertSame(CAUSE, withCause.getCause());
    }

    @Test
    void modelInference_carriesModelMessageAndCause() {
        ModelInferenceException withoutCause = new ModelInferenceException("face.onnx", "inference failed");
        ModelInferenceException withCause = new ModelInferenceException("face.onnx", "inference failed", CAUSE);

        assertEquals("face.onnx", withoutCause.modelIdentifier());
        assertEquals("inference failed", withoutCause.getMessage());
        assertSame(CAUSE, withCause.getCause());
    }

    @Test
    void persistence_carriesOperationMessageAndCause() {
        PersistenceException withoutCause = new PersistenceException("findById", "read failed");
        PersistenceException withCause = new PersistenceException("findById", "read failed", CAUSE);

        assertEquals("findById", withoutCause.operation());
        assertEquals("read failed", withoutCause.getMessage());
        assertSame(CAUSE, withCause.getCause());
    }

    @Test
    void configuration_carriesKeyMessageAndCause() {
        ConfigurationException withoutCause = new ConfigurationException("camera.index", "missing");
        ConfigurationException withCause = new ConfigurationException("camera.index", "missing", CAUSE);

        assertEquals("camera.index", withoutCause.configKey());
        assertEquals("missing", withoutCause.getMessage());
        assertSame(CAUSE, withCause.getCause());
    }
}
