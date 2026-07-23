package id.asr.rppgvitals.presentation.javafx.dashboard;

import javafx.application.Platform;

/// The production [UiThreadExecutor]: marshals actions onto the JavaFX Application Thread via
/// `Platform.runLater` (`11_THREADING.md §9`). Excluded from the coverage gate because it requires a
/// running JavaFX toolkit; the marshaling behaviour is exercised through the ViewModels' tests with a
/// synchronous executor (ADR 0009 §3).
public final class PlatformUiThreadExecutor implements UiThreadExecutor {

    /// Creates the executor.
    public PlatformUiThreadExecutor() {}

    /// {@inheritDoc}
    @Override
    public void runOnUiThread(Runnable action) {
        Platform.runLater(action);
    }
}
