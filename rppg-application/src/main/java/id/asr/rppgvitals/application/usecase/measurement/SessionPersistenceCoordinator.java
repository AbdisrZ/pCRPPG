package id.asr.rppgvitals.application.usecase.measurement;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import id.asr.rppgvitals.domain.session.MeasurementSession;

/// Runs end-of-session persistence off the caller's thread (`11_THREADING.md §2`, `§7`).
///
/// Neither the live pipeline nor the JavaFX Application Thread may block on the single end-of-session
/// database write (`10_DATABASE.md §6`). This coordinator dispatches that write onto the injected
/// `persistenceExecutor` — the third executor of the topology (`11 §2`), owned and shut down by the
/// composition root (`11 §8`) — and reports completion through callbacks that the caller marshals onto
/// its own thread. A synchronous variant is provided for deterministic application-exit flushing where
/// no further asynchronous work may be scheduled (`11 §8`).
public final class SessionPersistenceCoordinator {

    private final EndMeasurementSessionUseCase endSessionUseCase;
    private final Executor persistenceExecutor;

    /// Creates the coordinator.
    ///
    /// @param endSessionUseCase the use case that finalises and writes a session; never `null`
    /// @param persistenceExecutor the executor persistence tasks run on (`11 §2`); never `null`
    public SessionPersistenceCoordinator(EndMeasurementSessionUseCase endSessionUseCase, Executor persistenceExecutor) {
        this.endSessionUseCase = Objects.requireNonNull(endSessionUseCase, "endSessionUseCase");
        this.persistenceExecutor = Objects.requireNonNull(persistenceExecutor, "persistenceExecutor");
    }

    /// Ends and persists the session asynchronously on the persistence executor. Exactly one of the two
    /// callbacks runs, on the persistence thread, once the write settles.
    ///
    /// @param session the session to finalise; never `null`
    /// @param onSaved run after the session is written successfully; never `null`
    /// @param onError run with the failure if the write throws; never `null`
    public void endAsync(MeasurementSession session, Runnable onSaved, Consumer<? super RuntimeException> onError) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(onSaved, "onSaved");
        Objects.requireNonNull(onError, "onError");
        persistenceExecutor.execute(() -> {
            try {
                endSessionUseCase.execute(session);
            } catch (RuntimeException failure) {
                onError.accept(failure);
                return;
            }
            onSaved.run();
        });
    }

    /// Ends and persists the session on the calling thread, for the application-exit flush of `11 §8`
    /// where a measurement must not be lost and no new asynchronous task may be scheduled.
    ///
    /// @param session the session to finalise; never `null`
    public void endNow(MeasurementSession session) {
        Objects.requireNonNull(session, "session");
        endSessionUseCase.execute(session);
    }
}
