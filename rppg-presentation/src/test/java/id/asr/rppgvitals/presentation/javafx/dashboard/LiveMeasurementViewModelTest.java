package id.asr.rppgvitals.presentation.javafx.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.application.usecase.device.ListAvailableCameraDevicesUseCase;
import id.asr.rppgvitals.application.usecase.measurement.EndMeasurementSessionUseCase;
import id.asr.rppgvitals.application.usecase.measurement.LiveMeasurementOrchestrator;
import id.asr.rppgvitals.application.usecase.measurement.SessionPersistenceCoordinator;
import id.asr.rppgvitals.application.usecase.measurement.StartMeasurementSessionUseCase;
import id.asr.rppgvitals.domain.capture.CaptureConfiguration;
import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;
import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.signal.SignalQuality;

class LiveMeasurementViewModelTest {

    private static final Instant NOW = Instant.parse("2026-07-23T09:00:00Z");

    private final StartMeasurementSessionUseCase startUseCase = mock(StartMeasurementSessionUseCase.class);
    private final EndMeasurementSessionUseCase endUseCase = mock(EndMeasurementSessionUseCase.class);
    private final ListAvailableCameraDevicesUseCase listUseCase = mock(ListAvailableCameraDevicesUseCase.class);
    private final LiveMeasurementOrchestrator orchestrator = mock(LiveMeasurementOrchestrator.class);
    // Same-thread executors for both persistence and UI marshaling: callbacks apply immediately, so the
    // ViewModel is deterministically testable without a JavaFX toolkit or a live persistence thread.
    private final SessionPersistenceCoordinator persistence =
            new SessionPersistenceCoordinator(endUseCase, Runnable::run);
    private final LiveMeasurementViewModel viewModel =
            new LiveMeasurementViewModel(startUseCase, persistence, listUseCase, orchestrator, Runnable::run);

    @Test
    void onHeartRateUpdated_showsTheReadingAndItsTier() {
        viewModel.onHeartRateUpdated(new HeartRateEstimate(71.6, 0.9, NOW, "CHROM"));

        assertEquals(72, viewModel.currentHeartRateBpmProperty().get());
        assertEquals(ConfidenceTier.HIGH, viewModel.confidenceTierProperty().get());
        assertEquals("Reading stable", viewModel.signalStatusMessageProperty().get());
    }

    @Test
    void onSignalQualityChanged_toSearching_clearsTheReading() {
        viewModel.onHeartRateUpdated(new HeartRateEstimate(72.0, 0.9, NOW, "CHROM"));

        viewModel.onSignalQualityChanged(new SignalQuality.Searching());

        assertNull(viewModel.currentHeartRateBpmProperty().get());
        assertNull(viewModel.confidenceTierProperty().get());
        assertTrue(viewModel.signalStatusMessageProperty().get().startsWith("Finding your pulse"));
    }

    @Test
    void onSignalQualityChanged_handlesStableAndDegraded() {
        viewModel.onSignalQualityChanged(new SignalQuality.Stable());
        viewModel.onSignalQualityChanged(new SignalQuality.Degraded("insufficient lighting"));

        assertNull(viewModel.currentHeartRateBpmProperty().get());
        assertEquals(
                "Lighting too low — move to a brighter area",
                viewModel.signalStatusMessageProperty().get());
    }

    @Test
    void onSessionDegraded_selectsTheMessageFromTheReason() {
        viewModel.onSessionDegraded("camera lost");
        assertEquals(
                "Camera disconnected — reconnect to continue",
                viewModel.signalStatusMessageProperty().get());

        viewModel.onSessionDegraded("insufficient lighting");
        assertEquals(
                "Lighting too low — move to a brighter area",
                viewModel.signalStatusMessageProperty().get());
    }

    @Test
    void onSessionRecovered_returnsToSearching() {
        viewModel.onSessionDegraded("camera lost");

        viewModel.onSessionRecovered();

        assertTrue(viewModel.signalStatusMessageProperty().get().startsWith("Finding your pulse"));
    }

    @Test
    void refreshDevices_populatesTheDeviceList() {
        when(listUseCase.execute()).thenReturn(List.of("cam-0", "cam-1"));

        viewModel.refreshDevices();

        assertEquals(List.of("cam-0", "cam-1"), viewModel.availableDevices());
    }

    @Test
    void startSession_createsASessionAndStartsThePipeline() {
        MeasurementSession session = new MeasurementSession(UUID.randomUUID(), "cam-0", NOW);
        when(startUseCase.execute("cam-0")).thenReturn(session);
        viewModel.selectedDeviceProperty().set("cam-0");

        viewModel.startSession();

        assertTrue(viewModel.sessionActiveProperty().get());
        verify(orchestrator).start(eq(session), eq(viewModel), any(CaptureConfiguration.class));
        assertTrue(viewModel.signalStatusMessageProperty().get().startsWith("Finding your pulse"));
    }

    @Test
    void startSession_withNoDeviceSelected_throws() {
        assertThrows(IllegalStateException.class, viewModel::startSession);
    }

    @Test
    void startSession_whileAlreadyActive_throws() {
        when(startUseCase.execute("cam-0")).thenReturn(new MeasurementSession(UUID.randomUUID(), "cam-0", NOW));
        viewModel.selectedDeviceProperty().set("cam-0");
        viewModel.startSession();

        assertThrows(IllegalStateException.class, viewModel::startSession);
    }

    @Test
    void endSession_stopsThePipelinePersistsAndResets() {
        MeasurementSession session = new MeasurementSession(UUID.randomUUID(), "cam-0", NOW);
        when(startUseCase.execute("cam-0")).thenReturn(session);
        viewModel.selectedDeviceProperty().set("cam-0");
        viewModel.startSession();

        viewModel.endSession();

        verify(orchestrator).stop();
        verify(endUseCase).execute(session);
        assertFalse(viewModel.sessionActiveProperty().get());
        assertEquals("Session saved", viewModel.signalStatusMessageProperty().get());
    }

    @Test
    void shutdown_withActiveSession_stopsThePipelineAndFlushesSynchronously() {
        MeasurementSession session = new MeasurementSession(UUID.randomUUID(), "cam-0", NOW);
        when(startUseCase.execute("cam-0")).thenReturn(session);
        viewModel.selectedDeviceProperty().set("cam-0");
        viewModel.startSession();

        viewModel.shutdown();

        verify(orchestrator).stop();
        verify(endUseCase).execute(session);
        assertFalse(viewModel.sessionActiveProperty().get());
    }

    @Test
    void onPreviewFrame_publishesTheLatestSnapshot() {
        Frame frame = new Frame(new byte[2 * 2 * Frame.CHANNELS], 2, 2, 0L, NOW);
        RegionOfInterest roi = new RegionOfInterest(0, 0, 1, 1, 0.9);

        viewModel.onPreviewFrame(frame, roi);

        PreviewSnapshot snapshot = viewModel.latestPreviewProperty().get();
        assertEquals(frame, snapshot.frame());
        assertEquals(roi, snapshot.roi());
    }

    @Test
    void onPreviewFrame_toleratesAnAbsentRegion() {
        Frame frame = new Frame(new byte[2 * 2 * Frame.CHANNELS], 2, 2, 0L, NOW);

        viewModel.onPreviewFrame(frame, null);

        assertNull(viewModel.latestPreviewProperty().get().roi());
    }

    @Test
    void endSession_clearsThePreview() {
        MeasurementSession session = new MeasurementSession(UUID.randomUUID(), "cam-0", NOW);
        when(startUseCase.execute("cam-0")).thenReturn(session);
        viewModel.selectedDeviceProperty().set("cam-0");
        viewModel.startSession();
        viewModel.onPreviewFrame(new Frame(new byte[2 * 2 * Frame.CHANNELS], 2, 2, 0L, NOW), null);

        viewModel.endSession();

        assertNull(viewModel.latestPreviewProperty().get());
    }

    @Test
    void shutdown_whenIdle_isANoOp() {
        viewModel.shutdown();

        verify(orchestrator, never()).stop();
        verify(endUseCase, never()).execute(any());
        assertFalse(viewModel.sessionActiveProperty().get());
    }
}
