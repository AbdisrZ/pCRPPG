package id.asr.rppgvitals.application.usecase.history;

import java.util.List;
import java.util.Objects;

import id.asr.rppgvitals.domain.session.MeasurementRepository;
import id.asr.rppgvitals.domain.session.SessionSummary;

/// Lists stored measurement sessions for the history screen (`03_ARCHITECTURE.md §6.2`,
/// `02_SOFTWARE_REQUIREMENT.md` FR-202).
///
/// A discrete use case: it orchestrates a single call to the [MeasurementRepository] port and does no
/// computation of its own (`00_MASTER_PROMPT.md §9`).
public final class ListSessionHistoryUseCase {

    private final MeasurementRepository repository;

    /// Creates the use case.
    ///
    /// @param repository the persistence port to read summaries from; never `null`
    public ListSessionHistoryUseCase(MeasurementRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /// Returns every stored session as a lightweight summary, newest first.
    ///
    /// @return the session summaries, most recent first; empty when no session has been stored
    public List<SessionSummary> execute() {
        return repository.findAllSummaries();
    }
}
