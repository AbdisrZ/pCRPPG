package id.asr.rppgvitals.presentation.javafx.dashboard;

/// Runs an action on the JavaFX Application Thread (`11_THREADING.md §9`).
///
/// The `MeasurementObserver` callbacks the live ViewModel implements arrive on the processing thread;
/// every UI-state mutation must be marshalled onto the FX thread before touching a `Property`. This
/// interface abstracts that single hand-off (the production implementation wraps `Platform.runLater`)
/// so a ViewModel can be unit-tested with a synchronous executor and no running JavaFX toolkit.
@FunctionalInterface
public interface UiThreadExecutor {

    /// Schedules the action to run on the JavaFX Application Thread.
    ///
    /// @param action the work to run on the UI thread; never `null`
    void runOnUiThread(Runnable action);
}
