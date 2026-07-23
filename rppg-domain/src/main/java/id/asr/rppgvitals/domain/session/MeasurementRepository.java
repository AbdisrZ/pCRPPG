package id.asr.rppgvitals.domain.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import id.asr.rppgvitals.domain.exception.PersistenceException;

/// Domain port for measurement-session persistence (`03_ARCHITECTURE.md §4`, tracing
/// `02_SOFTWARE_REQUIREMENT.md §3.2`). Implemented in the infrastructure layer by
/// `SqliteMeasurementRepository`.
///
/// The interface is deliberately narrow — persist, list, retrieve, delete — per the Interface
/// Segregation guidance of `00_MASTER_PROMPT.md §21.1`. All methods translate infrastructure
/// failures into [PersistenceException] (`00 §22.1`); no checked or vendor persistence type crosses
/// this boundary.
public interface MeasurementRepository {

    /// Persists a completed session.
    ///
    /// @param session the session to persist; never `null`
    /// @throws PersistenceException if the session cannot be stored
    void save(MeasurementSession session);

    /// Lists lightweight summaries of all stored sessions for history display.
    ///
    /// @return an immutable list of summaries, most useful ordered by start time; empty when none exist
    /// @throws PersistenceException if the summaries cannot be read
    List<SessionSummary> findAllSummaries();

    /// Retrieves a full session by its identity.
    ///
    /// @param id the identity of the session to retrieve; never `null`
    /// @return the session, or [Optional#empty()] if no session has that id
    /// @throws PersistenceException if the session cannot be read
    Optional<MeasurementSession> findById(UUID id);

    /// Deletes a stored session by its identity. Deleting an absent session is a no-op.
    ///
    /// @param id the identity of the session to delete; never `null`
    /// @throws PersistenceException if the deletion fails
    void deleteById(UUID id);
}
