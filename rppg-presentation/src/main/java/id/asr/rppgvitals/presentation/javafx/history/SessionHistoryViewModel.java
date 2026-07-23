package id.asr.rppgvitals.presentation.javafx.history;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import id.asr.rppgvitals.application.usecase.history.DeleteSessionUseCase;
import id.asr.rppgvitals.application.usecase.history.GetSessionDetailUseCase;
import id.asr.rppgvitals.application.usecase.history.ListSessionHistoryUseCase;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.session.SessionSummary;

/// The ViewModel for the Session History screen (`06_UI_GUIDELINE.md §6.3`, `03_ARCHITECTURE.md §6.2`).
///
/// It exposes the stored sessions as an observable list for the View to bind to, and delegates every
/// action — list, delete, open detail — to the application use cases (`06 §8`). A row's confidence
/// encoding is derived from its [SessionSummary#meanConfidence] via
/// [id.asr.rppgvitals.presentation.javafx.dashboard.ConfidenceTier], the same tiering the live screen
/// uses (`06 §6.3`).
///
/// Its own screen and lifecycle, independent of the live ViewModel (`06 §8`). Delete requires a
/// confirmation step in the View before this ViewModel is asked to perform it (`06 §6.3`,
/// `02` FR-204).
public final class SessionHistoryViewModel {

    private final ListSessionHistoryUseCase listSessionHistoryUseCase;
    private final DeleteSessionUseCase deleteSessionUseCase;
    private final GetSessionDetailUseCase getSessionDetailUseCase;

    private final ObservableList<SessionSummary> sessions = FXCollections.observableArrayList();
    private final ObjectProperty<SessionSummary> selectedSession = new SimpleObjectProperty<>();

    /// Creates the ViewModel with its application-layer collaborators.
    ///
    /// @param listSessionHistoryUseCase lists stored session summaries; never `null`
    /// @param deleteSessionUseCase deletes a session; never `null`
    /// @param getSessionDetailUseCase retrieves a full session for the detail screen; never `null`
    public SessionHistoryViewModel(
            ListSessionHistoryUseCase listSessionHistoryUseCase,
            DeleteSessionUseCase deleteSessionUseCase,
            GetSessionDetailUseCase getSessionDetailUseCase) {
        this.listSessionHistoryUseCase = Objects.requireNonNull(listSessionHistoryUseCase, "listSessionHistoryUseCase");
        this.deleteSessionUseCase = Objects.requireNonNull(deleteSessionUseCase, "deleteSessionUseCase");
        this.getSessionDetailUseCase = Objects.requireNonNull(getSessionDetailUseCase, "getSessionDetailUseCase");
    }

    /// Reloads the session list, most recent first (`02` FR-202).
    public void refresh() {
        sessions.setAll(listSessionHistoryUseCase.execute());
    }

    /// Deletes the session with the given identity, then reloads the list (`02` FR-204).
    ///
    /// @param sessionId the identity of the session to delete; never `null`
    public void delete(UUID sessionId) {
        deleteSessionUseCase.execute(sessionId);
        refresh();
    }

    /// Retrieves the full detail of a session for the detail screen (`02` FR-203).
    ///
    /// @param sessionId the identity of the session to open; never `null`
    /// @return the session, or [Optional#empty()] if it no longer exists
    public Optional<MeasurementSession> sessionDetail(UUID sessionId) {
        return getSessionDetailUseCase.execute(sessionId);
    }

    /// The stored session summaries the history list binds to.
    ///
    /// @return the observable list of session summaries
    public ObservableList<SessionSummary> sessions() {
        return sessions;
    }

    /// The currently selected session summary, or `null` if none is selected.
    ///
    /// @return the bindable selected-session property
    public ObjectProperty<SessionSummary> selectedSessionProperty() {
        return selectedSession;
    }
}
