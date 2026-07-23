package id.asr.rppgvitals.application.usecase.measurement;

import java.time.Clock;
import java.util.Objects;

import id.asr.rppgvitals.domain.session.MeasurementRepository;
import id.asr.rppgvitals.domain.session.MeasurementSession;

/// Ends a measurement session normally and persists it (`03_ARCHITECTURE.md §6.2`,
/// `02_SOFTWARE_REQUIREMENT.md` FR-107, FR-201).
///
/// It marks the session complete at the current time and writes it — with all its buffered estimates
/// — in the single end-of-session transaction of `10_DATABASE.md §6`. Stopping the live pipeline is
/// the `LiveMeasurementOrchestrator`'s responsibility (T-302); this use case owns the finalisation and
/// persistence.
public final class EndMeasurementSessionUseCase {

    private final MeasurementRepository repository;
    private final Clock clock;

    /// Creates the use case.
    ///
    /// @param repository the persistence port to save the completed session to; never `null`
    /// @param clock the clock supplying the session end time; never `null`
    public EndMeasurementSessionUseCase(MeasurementRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /// Completes the session at the current time and persists it.
    ///
    /// @param session the active session to end; never `null`
    /// @throws IllegalStateException if the session has already ended
    public void execute(MeasurementSession session) {
        Objects.requireNonNull(session, "session");
        session.complete(clock.instant());
        repository.save(session);
    }
}
