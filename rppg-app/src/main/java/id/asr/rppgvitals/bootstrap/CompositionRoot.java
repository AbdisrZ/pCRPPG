package id.asr.rppgvitals.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import id.asr.rppgvitals.application.usecase.device.ListAvailableCameraDevicesUseCase;
import id.asr.rppgvitals.application.usecase.history.DeleteSessionUseCase;
import id.asr.rppgvitals.application.usecase.history.GetSessionDetailUseCase;
import id.asr.rppgvitals.application.usecase.history.ListSessionHistoryUseCase;
import id.asr.rppgvitals.application.usecase.measurement.EndMeasurementSessionUseCase;
import id.asr.rppgvitals.application.usecase.measurement.LiveMeasurementOrchestrator;
import id.asr.rppgvitals.application.usecase.measurement.SessionPersistenceCoordinator;
import id.asr.rppgvitals.application.usecase.measurement.StartMeasurementSessionUseCase;
import id.asr.rppgvitals.domain.estimation.ChromSignalEstimator;
import id.asr.rppgvitals.domain.exception.ConfigurationException;
import id.asr.rppgvitals.domain.session.MeasurementRepository;
import id.asr.rppgvitals.infrastructure.capture.opencv.OpenCvFrameSource;
import id.asr.rppgvitals.infrastructure.persistence.sqlite.SqliteMeasurementRepository;
import id.asr.rppgvitals.presentation.javafx.dashboard.LiveMeasurementViewModel;
import id.asr.rppgvitals.presentation.javafx.dashboard.PlatformUiThreadExecutor;
import id.asr.rppgvitals.presentation.javafx.history.SessionHistoryViewModel;

/// Assembles the application from its parts and owns their lifecycle (`03_ARCHITECTURE.md §8`,
/// `00_MASTER_PROMPT.md §30`): the SQLite connection, the OpenCV capture adapter, the placeholder
/// inference engine, the CHROM estimator, the orchestrator, the use cases, and the ViewModels — wired
/// by explicit constructor injection, no static singletons.
///
/// Not covered by the coverage gate: it is pure wiring plus process resources (a database file, a
/// camera), exercised when the app runs, not by unit tests.
final class CompositionRoot {

    private static final String APP_VERSION = "0.1.0";
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

    private final Connection connection;
    private final LiveMeasurementOrchestrator orchestrator;
    private final ExecutorService persistenceExecutor;
    private final LiveMeasurementViewModel liveMeasurementViewModel;
    private final SessionHistoryViewModel sessionHistoryViewModel;

    CompositionRoot() {
        this.connection = openDatabase();
        MeasurementRepository repository = new SqliteMeasurementRepository(connection, APP_VERSION);
        OpenCvFrameSource frameSource = new OpenCvFrameSource();
        this.orchestrator = new LiveMeasurementOrchestrator(
                frameSource, new HeuristicInferenceEngine(), new ChromSignalEstimator());
        this.persistenceExecutor = Executors.newVirtualThreadPerTaskExecutor();
        Clock clock = Clock.systemUTC();
        SessionPersistenceCoordinator persistence = new SessionPersistenceCoordinator(
                new EndMeasurementSessionUseCase(repository, clock), persistenceExecutor);
        this.liveMeasurementViewModel = new LiveMeasurementViewModel(
                new StartMeasurementSessionUseCase(clock),
                persistence,
                new ListAvailableCameraDevicesUseCase(frameSource),
                orchestrator,
                new PlatformUiThreadExecutor());
        this.sessionHistoryViewModel = new SessionHistoryViewModel(
                new ListSessionHistoryUseCase(repository),
                new DeleteSessionUseCase(repository),
                new GetSessionDetailUseCase(repository));
    }

    LiveMeasurementViewModel liveMeasurementViewModel() {
        return liveMeasurementViewModel;
    }

    SessionHistoryViewModel sessionHistoryViewModel() {
        return sessionHistoryViewModel;
    }

    void shutdown() {
        // 1. Flush an active session synchronously — the app must not exit mid-session and lose an
        //    unsaved measurement (11 §8 step 1).
        try {
            liveMeasurementViewModel.shutdown();
        } catch (RuntimeException flushing) {
            // Best effort: proceed with orderly teardown regardless of a failed final write.
        }
        // 2. Shut down every executor with a bounded wait (11 §8 step 2): the orchestrator's
        //    capture + processing pair, then the persistence executor (draining any in-flight save).
        orchestrator.close();
        shutdownExecutor(persistenceExecutor);
        // 3. Close the SQLite connection last, once nothing can still write to it (11 §8 step 3).
        try {
            connection.close();
        } catch (SQLException closing) {
            // Best effort on exit; nothing further can be done with a failed close.
        }
    }

    private static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static Connection openDatabase() {
        Path databaseFile = databaseFile();
        Path parent = databaseFile.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        } catch (IOException | SQLException cause) {
            throw new ConfigurationException("database", "cannot open the vitals database at " + databaseFile, cause);
        }
    }

    private static Path databaseFile() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        Path base;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = Path.of(appData != null ? appData : home);
        } else if (os.contains("mac")) {
            base = Path.of(home, "Library", "Application Support");
        } else {
            base = Path.of(home, ".local", "share");
        }
        return base.resolve("rppgvitals").resolve("vitals.db");
    }
}
