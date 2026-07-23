package id.asr.rppgvitals.bootstrap;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import id.asr.rppgvitals.presentation.javafx.dashboard.LiveMeasurementController;

/// The application entry point and JavaFX launcher (`03_ARCHITECTURE.md §8`).
///
/// It builds the [CompositionRoot], loads the live-measurement view, injects its ViewModel, and shows
/// the window; on exit it shuts the composition down deterministically (`11_THREADING.md §8`). Not
/// covered by the coverage gate (launches a UI; verified by running the app).
public final class Main extends Application {

    private CompositionRoot composition;

    /// Creates the application instance (invoked reflectively by the JavaFX launcher).
    public Main() {}

    /// {@inheritDoc}
    @Override
    public void start(Stage stage) throws IOException {
        composition = new CompositionRoot();
        FXMLLoader loader = new FXMLLoader(LiveMeasurementController.class.getResource("live-measurement.fxml"));
        Parent root = loader.load();
        LiveMeasurementController controller = loader.getController();
        controller.setViewModel(composition.liveMeasurementViewModel());
        stage.setTitle("rPPG Vitals Monitor");
        stage.setScene(new Scene(root, 460.0, 380.0));
        stage.show();
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
