package id.asr.rppgvitals.application.usecase.history;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import id.asr.rppgvitals.domain.session.MeasurementRepository;
import id.asr.rppgvitals.domain.session.MeasurementSession;

/// Retrieves the full detail of one stored session for the detail/trend screen
/// (`03_ARCHITECTURE.md §6.2`, `02_SOFTWARE_REQUIREMENT.md` FR-203).
///
/// A discrete use case delegating to the [MeasurementRepository] port.
public final class GetSessionDetailUseCase {

    private final MeasurementRepository repository;

    /// Creates the use case.
    ///
    /// @param repository the persistence port to read the session from; never `null`
    public GetSessionDetailUseCase(MeasurementRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /// Retrieves the session with the given identity, including its recorded estimates.
    ///
    /// @param sessionId the identity of the session to retrieve; never `null`
    /// @return the session, or [Optional#empty()] if no session has that identity
    public Optional<MeasurementSession> execute(UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        return repository.findById(sessionId);
    }
}
