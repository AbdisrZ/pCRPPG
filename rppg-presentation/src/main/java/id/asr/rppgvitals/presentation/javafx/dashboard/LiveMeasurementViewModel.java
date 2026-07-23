package id.asr.rppgvitals.presentation.javafx.dashboard;

import java.util.Locale;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import id.asr.rppgvitals.application.usecase.device.ListAvailableCameraDevicesUseCase;
import id.asr.rppgvitals.application.usecase.measurement.EndMeasurementSessionUseCase;
import id.asr.rppgvitals.application.usecase.measurement.LiveMeasurementOrchestrator;
import id.asr.rppgvitals.application.usecase.measurement.MeasurementObserver;
import id.asr.rppgvitals.application.usecase.measurement.StartMeasurementSessionUseCase;
import id.asr.rppgvitals.domain.capture.CaptureConfiguration;
import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.signal.SignalQuality;

/// The ViewModel for the Live Measurement screen (`06_UI_GUIDELINE.md §6.2`, `03_ARCHITECTURE.md §6.2`).
///
/// It is the presentation-side coordinator: the View binds to its `Property` fields and delegates every
/// action to it (`06 §8`, `00_MASTER_PROMPT.md §24`), while it drives the application layer — creating a
/// session, running the [LiveMeasurementOrchestrator], and finalising via the use cases. It implements
/// [MeasurementObserver]; each callback arrives on the processing thread and is marshalled onto the
/// JavaFX Application Thread through the injected [UiThreadExecutor] before any `Property` is touched
/// (`11_THREADING.md §9`).
///
/// A numeric reading and confidence tier are shown only while a value exists (signal `STABLE`); during
/// searching or degradation the number is cleared and a guidance message shown instead (`06 §3`, `§6.2`).
public final class LiveMeasurementViewModel implements MeasurementObserver {

    private static final int DEFAULT_FRAME_WIDTH = 640;
    private static final int DEFAULT_FRAME_HEIGHT = 480;
    private static final int DEFAULT_FRAME_RATE = 30;

    private static final String IDLE_MESSAGE = "Select a camera and press Start";
    private static final String SEARCHING_MESSAGE = "Finding your pulse — hold still and face the camera";
    private static final String SAVED_MESSAGE = "Session saved";
    private static final String CAMERA_LOST_MESSAGE = "Camera disconnected — reconnect to continue";
    private static final String LOW_LIGHT_MESSAGE = "Lighting too low — move to a brighter area";

    private final StartMeasurementSessionUseCase startSessionUseCase;
    private final EndMeasurementSessionUseCase endSessionUseCase;
    private final ListAvailableCameraDevicesUseCase listDevicesUseCase;
    private final LiveMeasurementOrchestrator orchestrator;
    private final UiThreadExecutor uiThreadExecutor;

    private final ObjectProperty<Integer> currentHeartRateBpm = new SimpleObjectProperty<>();
    private final ObjectProperty<ConfidenceTier> confidenceTier = new SimpleObjectProperty<>();
    private final StringProperty signalStatusMessage = new SimpleStringProperty(IDLE_MESSAGE);
    private final BooleanProperty sessionActive = new SimpleBooleanProperty(false);
    private final ObservableList<String> availableDevices = FXCollections.observableArrayList();
    private final StringProperty selectedDevice = new SimpleStringProperty();

    private MeasurementSession currentSession;

    /// Creates the ViewModel with its application-layer collaborators.
    ///
    /// @param startSessionUseCase creates a new session; never `null`
    /// @param endSessionUseCase finalises and persists a session; never `null`
    /// @param listDevicesUseCase enumerates camera devices; never `null`
    /// @param orchestrator the live capture/estimation pipeline; never `null`
    /// @param uiThreadExecutor marshals callbacks onto the JavaFX Application Thread; never `null`
    public LiveMeasurementViewModel(
            StartMeasurementSessionUseCase startSessionUseCase,
            EndMeasurementSessionUseCase endSessionUseCase,
            ListAvailableCameraDevicesUseCase listDevicesUseCase,
            LiveMeasurementOrchestrator orchestrator,
            UiThreadExecutor uiThreadExecutor) {
        this.startSessionUseCase = Objects.requireNonNull(startSessionUseCase, "startSessionUseCase");
        this.endSessionUseCase = Objects.requireNonNull(endSessionUseCase, "endSessionUseCase");
        this.listDevicesUseCase = Objects.requireNonNull(listDevicesUseCase, "listDevicesUseCase");
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
        this.uiThreadExecutor = Objects.requireNonNull(uiThreadExecutor, "uiThreadExecutor");
    }

    /// Refreshes the list of selectable camera devices (`02` FR-501).
    public void refreshDevices() {
        availableDevices.setAll(listDevicesUseCase.execute());
    }

    /// Starts a session against the selected device and begins the live pipeline (`02` FR-101).
    ///
    /// @throws IllegalStateException if a session is already active or no device is selected
    public void startSession() {
        if (sessionActive.get()) {
            throw new IllegalStateException("a session is already active");
        }
        String device = selectedDevice.get();
        if (device == null || device.isBlank()) {
            throw new IllegalStateException("no camera device selected");
        }
        currentSession = startSessionUseCase.execute(device);
        orchestrator.start(
                currentSession,
                this,
                new CaptureConfiguration(device, DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT, DEFAULT_FRAME_RATE));
        sessionActive.set(true);
        currentHeartRateBpm.set(null);
        confidenceTier.set(null);
        signalStatusMessage.set(SEARCHING_MESSAGE);
    }

    /// Ends the active session: stops the pipeline and persists it (`02` FR-107).
    public void endSession() {
        orchestrator.stop();
        endSessionUseCase.execute(currentSession);
        currentSession = null;
        sessionActive.set(false);
        currentHeartRateBpm.set(null);
        confidenceTier.set(null);
        signalStatusMessage.set(SAVED_MESSAGE);
    }

    /// {@inheritDoc}
    @Override
    public void onHeartRateUpdated(HeartRateEstimate estimate) {
        ConfidenceTier tier = ConfidenceTier.fromConfidence(estimate.confidence());
        int bpm = (int) Math.round(estimate.beatsPerMinute());
        uiThreadExecutor.runOnUiThread(() -> {
            currentHeartRateBpm.set(bpm);
            confidenceTier.set(tier);
            signalStatusMessage.set(tier.message());
        });
    }

    /// {@inheritDoc}
    @Override
    public void onSignalQualityChanged(SignalQuality quality) {
        uiThreadExecutor.runOnUiThread(() -> {
            switch (quality) {
                case SignalQuality.Searching ignored -> clearReading(SEARCHING_MESSAGE);
                case SignalQuality.Degraded degraded -> clearReading(degradedMessage(degraded.reason()));
                case SignalQuality.Stable ignored -> {
                    // The heart-rate update owns the message while stable; nothing to do here.
                }
            }
        });
    }

    /// {@inheritDoc}
    @Override
    public void onSessionDegraded(String reason) {
        String message = degradedMessage(reason);
        uiThreadExecutor.runOnUiThread(() -> clearReading(message));
    }

    /// {@inheritDoc}
    @Override
    public void onSessionRecovered() {
        uiThreadExecutor.runOnUiThread(() -> clearReading(SEARCHING_MESSAGE));
    }

    private void clearReading(String message) {
        currentHeartRateBpm.set(null);
        confidenceTier.set(null);
        signalStatusMessage.set(message);
    }

    private static String degradedMessage(String reason) {
        String lower = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        return lower.contains("light") ? LOW_LIGHT_MESSAGE : CAMERA_LOST_MESSAGE;
    }

    /// The current heart-rate reading in bpm, or `null` while no value exists.
    ///
    /// @return the bindable heart-rate property
    public ObjectProperty<Integer> currentHeartRateBpmProperty() {
        return currentHeartRateBpm;
    }

    /// The confidence tier of the current reading, or `null` while no value exists.
    ///
    /// @return the bindable confidence-tier property
    public ObjectProperty<ConfidenceTier> confidenceTierProperty() {
        return confidenceTier;
    }

    /// The guidance/status message for the current state.
    ///
    /// @return the bindable status-message property
    public StringProperty signalStatusMessageProperty() {
        return signalStatusMessage;
    }

    /// Whether a session is currently active.
    ///
    /// @return the bindable session-active property
    public BooleanProperty sessionActiveProperty() {
        return sessionActive;
    }

    /// The selectable camera device identifiers.
    ///
    /// @return the observable device list the view binds to
    public ObservableList<String> availableDevices() {
        return availableDevices;
    }

    /// The currently selected camera device identifier.
    ///
    /// @return the bindable selected-device property
    public StringProperty selectedDeviceProperty() {
        return selectedDevice;
    }
}
