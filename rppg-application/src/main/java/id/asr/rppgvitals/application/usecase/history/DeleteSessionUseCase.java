package id.asr.rppgvitals.application.usecase.history;

import java.util.Objects;
import java.util.UUID;

import id.asr.rppgvitals.domain.session.MeasurementRepository;

/// Deletes a stored session at the user's request (`03_ARCHITECTURE.md §6.2`,
/// `02_SOFTWARE_REQUIREMENT.md` FR-204).
///
/// A discrete use case delegating to the [MeasurementRepository] port, which removes the session and
/// its samples together (`10_DATABASE.md §9`).
public final class DeleteSessionUseCase {

    private final MeasurementRepository repository;

    /// Creates the use case.
    ///
    /// @param repository the persistence port to delete from; never `null`
    public DeleteSessionUseCase(MeasurementRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /// Deletes the session with the given identity. Deleting an absent session is a no-op.
    ///
    /// @param sessionId the identity of the session to delete; never `null`
    public void execute(UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        repository.deleteById(sessionId);
    }
}
