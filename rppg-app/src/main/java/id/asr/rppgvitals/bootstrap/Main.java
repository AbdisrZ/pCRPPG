package id.asr.rppgvitals.bootstrap;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import id.asr.rppgvitals.presentation.javafx.dashboard.LiveMeasurementController;
import id.asr.rppgvitals.presentation.javafx.dashboard.ScreenNavigator;
import id.asr.rppgvitals.presentation.javafx.history.SessionHistoryController;

/// The application entry point, JavaFX launcher, and screen navigator (`03_ARCHITECTURE.md §8`,
/// `06_UI_GUIDELINE.md §5`).
///
/// It builds the [CompositionRoot], loads the Live Measurement and Session History views once, injects
/// their ViewModels and this navigator, and shows the window; navigation swaps the single scene's root
/// between the two. On exit it shuts the composition down deterministically (`11_THREADING.md §8`). Not
/// covered by the coverage gate (launches a UI; verified by running the app).
public final class Main extends Application implements ScreenNavigator {

    private CompositionRoot composition;
    private Scene scene;
    private Parent liveRoot;
    private Parent historyRoot;

    /// Creates the application instance (invoked reflectively by the JavaFX launcher).
    public Main() {}

    /// {@inheritDoc}
    @Override
    public void start(Stage stage) throws IOException {
        composition = new CompositionRoot();

        FXMLLoader liveLoader = new FXMLLoader(LiveMeasurementController.class.getResource("live-measurement.fxml"));
        this.liveRoot = liveLoader.load();
        LiveMeasurementController liveController = liveLoader.getController();
        liveController.initView(composition.liveMeasurementViewModel(), this);

        FXMLLoader historyLoader = new FXMLLoader(SessionHistoryController.class.getResource("session-history.fxml"));
        this.historyRoot = historyLoader.load();
        SessionHistoryController historyController = historyLoader.getController();
        historyController.initView(composition.sessionHistoryViewModel(), this);

        this.scene = new Scene(liveRoot, 480.0, 440.0);
        stage.setTitle("rPPG Vitals Monitor");
        stage.setScene(scene);
        stage.show();
    }

    /// {@inheritDoc}
    @Override
    public void showLiveMeasurement() {
        scene.setRoot(liveRoot);
    }

    /// {@inheritDoc}
    @Override
    public void showSessionHistory() {
        composition.sessionHistoryViewModel().refresh();
        scene.setRoot(historyRoot);
    }

    /// {@inheritDoc}
    @Override
    public void stop() {
        if (composition != null) {
            composition.shutdown();
        }
    }

    /// Launches the application.
    ///
    /// @param args the command-line arguments passed to the JavaFX launcher
    public static void main(String[] args) {
        launch(args);
    }
}
