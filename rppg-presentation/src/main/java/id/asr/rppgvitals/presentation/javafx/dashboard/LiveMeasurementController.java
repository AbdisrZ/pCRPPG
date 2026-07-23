package id.asr.rppgvitals.presentation.javafx.dashboard;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;

/// The JavaFX controller wiring `live-measurement.fxml` to the [LiveMeasurementViewModel]
/// (`06_UI_GUIDELINE.md §6.2`, `00_MASTER_PROMPT.md §24`).
///
/// It contains no business logic: it binds the view's controls to the ViewModel's `Property` state and
/// forwards user actions to the ViewModel. Excluded from the coverage gate (requires a running JavaFX
/// toolkit; verified via the manual UI checklist, `13_TESTING.md §4`).
public final class LiveMeasurementController {

    private static final int TREND_CAPACITY = 240;
    private static final double TREND_MIN_BPM = 40.0;
    private static final double TREND_MAX_BPM = 180.0;

    @FXML
    private ComboBox<String> deviceSelector;

    @FXML
    private Button startStopButton;

    @FXML
    private Label heartRateLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Canvas previewCanvas;

    @FXML
    private Canvas trendCanvas;

    private LiveMeasurementViewModel viewModel;
    private ScreenNavigator navigator;
    private WritableImage previewImage;
    private final Deque<Integer> heartRateHistory = new ArrayDeque<>();

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

        renderPreview(null);
        viewModel.latestPreviewProperty().addListener((observable, previous, snapshot) -> renderPreview(snapshot));

        renderTrend();
        viewModel.currentHeartRateBpmProperty().addListener((observable, previous, bpm) -> onHeartRate(bpm));
        viewModel.sessionActiveProperty().addListener((observable, was, active) -> {
            if (Boolean.TRUE.equals(active)) {
                heartRateHistory.clear();
                renderTrend();
            }
        });

        viewModel.refreshDevices();
    }

    private void onHeartRate(Integer bpm) {
        if (bpm != null) {
            if (heartRateHistory.size() >= TREND_CAPACITY) {
                heartRateHistory.removeFirst();
            }
            heartRateHistory.addLast(bpm);
        }
        renderTrend();
    }

    private void renderPreview(PreviewSnapshot snapshot) {
        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        double width = previewCanvas.getWidth();
        double height = previewCanvas.getHeight();
        if (snapshot == null) {
            gc.setFill(Color.web("#1B1B1B"));
            gc.fillRect(0.0, 0.0, width, height);
            return;
        }
        Frame frame = snapshot.frame();
        gc.drawImage(imageOf(frame), 0.0, 0.0, width, height);
        drawRegion(gc, snapshot.roi(), width / frame.width(), height / frame.height());
        drawReadout(gc);
    }

    private WritableImage imageOf(Frame frame) {
        int w = frame.width();
        int h = frame.height();
        if (previewImage == null || (int) previewImage.getWidth() != w || (int) previewImage.getHeight() != h) {
            previewImage = new WritableImage(w, h);
        }
        previewImage.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getByteRgbInstance(), frame.pixels(), 0, w * 3);
        return previewImage;
    }

    private void drawRegion(GraphicsContext gc, RegionOfInterest roi, double scaleX, double scaleY) {
        if (roi == null) {
            return;
        }
        double x = roi.x() * scaleX;
        double y = roi.y() * scaleY;
        double w = roi.width() * scaleX;
        double h = roi.height() * scaleY;
        gc.setStroke(Color.LIMEGREEN);
        gc.setLineWidth(2.0);
        gc.strokeRect(x, y, w, h);

        // Detection probability label sitting on top of the box, OpenCV-style.
        String label = String.format(Locale.ROOT, "face %.0f%%", roi.detectionConfidence() * 100.0);
        gc.setFont(Font.font("System", FontWeight.BOLD, 15.0));
        double labelY = y > 20.0 ? y - 6.0 : y + h + 16.0;
        gc.setFill(Color.color(0.0, 0.0, 0.0, 0.55));
        gc.fillRect(x, labelY - 14.0, 92.0, 18.0);
        gc.setFill(Color.LIMEGREEN);
        gc.fillText(label, x + 4.0, labelY);
    }

    private void renderTrend() {
        GraphicsContext gc = trendCanvas.getGraphicsContext2D();
        double width = trendCanvas.getWidth();
        double height = trendCanvas.getHeight();
        gc.setFill(Color.web("#101418"));
        gc.fillRect(0.0, 0.0, width, height);
        if (heartRateHistory.size() < 2) {
            return;
        }
        gc.setStroke(Color.web("#26C6DA"));
        gc.setLineWidth(2.0);
        gc.beginPath();
        int count = heartRateHistory.size();
        double stepX = width / (TREND_CAPACITY - 1);
        double startX = width - (count - 1) * stepX;
        int index = 0;
        for (int bpm : heartRateHistory) {
            double px = startX + index * stepX;
            double norm = (clampBpm(bpm) - TREND_MIN_BPM) / (TREND_MAX_BPM - TREND_MIN_BPM);
            double py = height - norm * height;
            if (index == 0) {
                gc.moveTo(px, py);
            } else {
                gc.lineTo(px, py);
            }
            index++;
        }
        gc.stroke();
    }

    private static double clampBpm(int bpm) {
        return Math.max(TREND_MIN_BPM, Math.min(TREND_MAX_BPM, bpm));
    }

    private void drawReadout(GraphicsContext gc) {
        Integer bpm = viewModel.currentHeartRateBpmProperty().get();
        ConfidenceTier tier = viewModel.confidenceTierProperty().get();
        String text = bpm == null ? "— — bpm" : bpm + " bpm";
        Color colour = tier == null ? Color.web("#B0BEC5") : Color.web(tier.colorHex());
        gc.setFill(Color.color(0.0, 0.0, 0.0, 0.45));
        gc.fillRect(6.0, 6.0, 132.0, 30.0);
        gc.setFill(colour);
        gc.setFont(Font.font("System", FontWeight.BOLD, 20.0));
        gc.fillText(text, 12.0, 28.0);
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
