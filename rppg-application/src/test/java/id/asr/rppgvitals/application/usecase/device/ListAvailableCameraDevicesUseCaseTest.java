package id.asr.rppgvitals.application.usecase.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.capture.FrameSource;

class ListAvailableCameraDevicesUseCaseTest {

    private final FrameSource frameSource = mock(FrameSource.class);

    @Test
    void execute_returnsTheDeviceIdentifiersFromTheFrameSource() {
        when(frameSource.availableDeviceIds()).thenReturn(List.of("cam-0", "cam-1"));

        List<String> devices = new ListAvailableCameraDevicesUseCase(frameSource).execute();

        assertEquals(List.of("cam-0", "cam-1"), devices);
    }

    @Test
    void constructor_withNullFrameSource_throws() {
        assertThrows(NullPointerException.class, () -> new ListAvailableCameraDevicesUseCase(null));
    }
}
