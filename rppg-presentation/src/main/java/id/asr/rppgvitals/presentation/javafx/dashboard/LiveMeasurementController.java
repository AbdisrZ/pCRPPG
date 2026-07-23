package id.asr.rppgvitals.presentation.javafx.dashboard;

import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

/// The JavaFX controller wiring `live-measurement.fxml` to the [LiveMeasurementViewModel]
/// (`06_UI_GUIDELINE.md §6.2`, `00_MASTER_PROMPT.md §24`).
///
/// It contains no business logic: it binds the view's controls to the ViewModel's `Property` state and
/// forwards user actions to the ViewModel. Excluded from the coverage gate (requires a running JavaFX
/// toolkit; verified via the manual UI checklist, `13_TESTING.md §4`).
public final class LiveMeasurementController {

    @FXML
    private ComboBox<String> deviceSelector;

    @FXML
    private Button startStopButton;

    @FXML
    private Label heartRateLabel;

    @FXML
    private Label statusLabel;

    private LiveMeasurementViewModel viewModel;
    private ScreenNavigator navigator;

    /// Creates the controller; instantiated by the JavaFX `FXMLLoader`.
    public LiveMeasurementController() {}

    /// Injects the ViewModel and navigator and binds the view. Called by the composition root after the
    /// FXML is loaded.
    ///
    /// @param viewModel the live-measurement ViewModel; never `null`
    /// @param navigator the screen navigator for the view-history action; never `null`
    public void initView(LiveMeasurementViewModel viewModel, ScreenNavigator navigator) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.navigator = Objects.requireNonNull(navigator, "navigator");
        bind();
    }

    private void bind() {
        deviceSelector.setItems(viewModel.availableDevices());
        viewModel
                .selectedDeviceProperty()
                .bind(deviceSelector.getSelectionModel().selectedItemProperty());

        heartRateLabel
                .textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            Integer bpm =
                                    viewModel.currentHeartRateBpmProperty().get();
                            return bpm == null ? "— —" : bpm + " bpm";
                        },
                        viewModel.currentHeartRateBpmProperty()));
        heartRateLabel
                .styleProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            ConfidenceTier tier =
                                    viewModel.confidenceTierProperty().get();
                            String colour = tier == null ? "#607D8B" : tier.colorHex();
                            return "-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: " + colour + ";";
                        },
                        viewModel.confidenceTierProperty()));

        statusLabel.textProperty().bind(viewModel.signalStatusMessageProperty());
        startStopButton
                .textProperty()
                .bind(Bindings.when(viewModel.sessionActiveProperty())
                        .then("End")
                        .otherwise("Start"));

        viewModel.refreshDevices();
    }

    @FXML
    void onStartStop() {
        if (viewModel.sessionActiveProperty().get()) {
            viewModel.endSession();
        } else {
            viewModel.startSession();
        }
    }

    @FXML
    void onRefreshDevices() {
        viewModel.refreshDevices();
    }

    @FXML
    void onViewHistory() {
        navigator.showSessionHistory();
    }
}
