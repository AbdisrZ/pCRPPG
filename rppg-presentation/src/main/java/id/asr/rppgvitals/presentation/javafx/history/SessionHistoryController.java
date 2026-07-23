package id.asr.rppgvitals.presentation.javafx.history;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import id.asr.rppgvitals.domain.session.SessionSummary;
import id.asr.rppgvitals.presentation.javafx.dashboard.ConfidenceTier;
import id.asr.rppgvitals.presentation.javafx.dashboard.ScreenNavigator;

/// The JavaFX controller wiring `session-history.fxml` to the [SessionHistoryViewModel]
/// (`06_UI_GUIDELINE.md §6.3`).
///
/// It binds the table to the ViewModel's session list, formats each row (date, duration, mean HR, mean
/// confidence), colours the mean-HR cell with the shared [ConfidenceTier] encoding, and gates deletion
/// behind an explicit confirmation dialog (`02` FR-204). Excluded from the coverage gate (requires a
/// running JavaFX toolkit; verified via the manual UI checklist, `13_TESTING.md §4`).
public final class SessionHistoryController {

    private static final String ABSENT = "—";
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);

    @FXML
    private TableView<SessionSummary> sessionsTable;

    @FXML
    private TableColumn<SessionSummary, String> dateColumn;

    @FXML
    private TableColumn<SessionSummary, String> durationColumn;

    @FXML
    private TableColumn<SessionSummary, SessionSummary> heartRateColumn;

    @FXML
    private TableColumn<SessionSummary, String> confidenceColumn;

    @FXML
    private Button deleteButton;

    @FXML
    private Label emptyLabel;

    private SessionHistoryViewModel viewModel;
    private ScreenNavigator navigator;

    /// Creates the controller; instantiated by the JavaFX `FXMLLoader`.
    public SessionHistoryController() {}

    /// Injects the ViewModel and navigator and binds the view. Called by the composition root after the
    /// FXML is loaded.
    ///
    /// @param viewModel the history ViewModel; never `null`
    /// @param navigator the screen navigator for the back action; never `null`
    public void initView(SessionHistoryViewModel viewModel, ScreenNavigator navigator) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.navigator = Objects.requireNonNull(navigator, "navigator");
        bind();
    }

    private void bind() {
        ObservableList<SessionSummary> sessions = viewModel.sessions();
        sessionsTable.setItems(sessions);
        viewModel
                .selectedSessionProperty()
                .bind(sessionsTable.getSelectionModel().selectedItemProperty());
        deleteButton
                .disableProperty()
                .bind(sessionsTable.getSelectionModel().selectedItemProperty().isNull());

        emptyLabel.visibleProperty().bind(Bindings.isEmpty(sessions));
        emptyLabel.managedProperty().bind(emptyLabel.visibleProperty());

        dateColumn.setCellValueFactory(row -> new SimpleStringProperty(formatStart(row.getValue())));
        durationColumn.setCellValueFactory(row -> new SimpleStringProperty(formatDuration(row.getValue())));
        confidenceColumn.setCellValueFactory(row -> new SimpleStringProperty(formatConfidence(row.getValue())));
        heartRateColumn.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue()));
        heartRateColumn.setCellFactory(column -> new HeartRateCell());
    }

    @FXML
    void onDelete() {
        SessionSummary selected = sessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete this session permanently? This cannot be undone.",
                ButtonType.CANCEL,
                ButtonType.OK);
        confirm.setHeaderText("Delete session");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isPresent() && choice.get() == ButtonType.OK) {
            viewModel.delete(selected.sessionId());
        }
    }

    @FXML
    void onBack() {
        navigator.showLiveMeasurement();
    }

    private static String formatStart(SessionSummary summary) {
        return DATE_TIME.format(summary.startedAt().atZone(ZoneId.systemDefault()));
    }

    private static String formatDuration(SessionSummary summary) {
        if (summary.endedAt() == null) {
            return ABSENT;
        }
        long seconds = Math.max(
                0L, Duration.between(summary.startedAt(), summary.endedAt()).toSeconds());
        return String.format("%d:%02d", seconds / 60L, seconds % 60L);
    }

    private static String formatHeartRate(SessionSummary summary) {
        Double bpm = summary.meanHeartRateBpm();
        return bpm == null ? ABSENT : Math.round(bpm) + " bpm";
    }

    private static String formatConfidence(SessionSummary summary) {
        Double confidence = summary.meanConfidence();
        return confidence == null ? ABSENT : Math.round(confidence * 100.0) + "%";
    }

    /// A mean-HR cell that colours the reading with the shared confidence-tier encoding (`06 §6.3`).
    private static final class HeartRateCell extends TableCell<SessionSummary, SessionSummary> {

        @Override
        protected void updateItem(SessionSummary summary, boolean empty) {
            super.updateItem(summary, empty);
            if (empty || summary == null) {
                setText(null);
                setStyle("");
                return;
            }
            setText(formatHeartRate(summary));
            Double confidence = summary.meanConfidence();
            if (confidence == null) {
                setStyle("");
            } else {
                setStyle("-fx-font-weight: bold; -fx-text-fill: "
                        + ConfidenceTier.fromConfidence(confidence).colorHex() + ";");
            }
        }
    }
}
