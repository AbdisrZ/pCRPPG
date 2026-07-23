package id.asr.rppgvitals.presentation.javafx.dashboard;

/// Switches the single application window between top-level screens (`06_UI_GUIDELINE.md §5`).
///
/// Controllers depend on this seam rather than on the JavaFX `Stage`/`Scene` directly, so a button press
/// that navigates ("view history", "back") stays free of window-management detail and the composition
/// root (`03_ARCHITECTURE.md §8`) owns how screens are actually swapped. Implemented by the launcher.
public interface ScreenNavigator {

    /// Shows the Live Measurement screen (`06 §6.2`).
    void showLiveMeasurement();

    /// Shows the Session History screen, refreshing its list first (`06 §6.3`).
    void showSessionHistory();
}
