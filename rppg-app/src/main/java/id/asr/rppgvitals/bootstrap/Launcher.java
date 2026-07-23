package id.asr.rppgvitals.bootstrap;

/// A thin entry point that does not extend `javafx.application.Application` (`03_ARCHITECTURE.md §8`).
///
/// When JavaFX is on the classpath rather than the module path, the JDK launcher refuses to start a
/// class that extends `Application` ("JavaFX runtime components are missing"). Routing `main` through a
/// non-`Application` class sidesteps that check, so the app runs from a plain classpath launch (an IDE
/// run configuration) as well as from `javafx:run`. Not covered by the coverage gate.
public final class Launcher {

    private Launcher() {}

    /// Launches the application by delegating to the JavaFX [Main].
    ///
    /// @param args the command-line arguments
    public static void main(String[] args) {
        Main.main(args);
    }
}
