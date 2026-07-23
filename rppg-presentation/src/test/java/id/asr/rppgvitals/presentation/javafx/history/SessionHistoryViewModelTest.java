package id.asr.rppgvitals.presentation.javafx.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.application.usecase.history.DeleteSessionUseCase;
import id.asr.rppgvitals.application.usecase.history.GetSessionDetailUseCase;
import id.asr.rppgvitals.application.usecase.history.ListSessionHistoryUseCase;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.session.SessionStatus;
import id.asr.rppgvitals.domain.session.SessionSummary;

class SessionHistoryViewModelTest {

    private static final Instant NOW = Instant.parse("2026-07-23T09:00:00Z");

    private final ListSessionHistoryUseCase listUseCase = mock(ListSessionHistoryUseCase.class);
    private final DeleteSessionUseCase deleteUseCase = mock(DeleteSessionUseCase.class);
    private final GetSessionDetailUseCase getDetailUseCase = mock(GetSessionDetailUseCase.class);
    private final SessionHistoryViewModel viewModel =
            new SessionHistoryViewModel(listUseCase, deleteUseCase, getDetailUseCase);

    private static SessionSummary summary(UUID id) {
        return new SessionSummary(id, NOW, NOW, SessionStatus.COMPLETED, 72.0, 0.8);
    }

    @Test
    void refresh_populatesTheSessionList() {
        SessionSummary summary = summary(UUID.randomUUID());
        when(listUseCase.execute()).thenReturn(List.of(summary));

        viewModel.refresh();

        assertEquals(List.of(summary), viewModel.sessions());
    }

    @Test
    void delete_removesTheSessionThenReloads() {
        UUID id = UUID.randomUUID();
        when(listUseCase.execute()).thenReturn(List.of());

        viewModel.delete(id);

        verify(deleteUseCase).execute(id);
        verify(listUseCase).execute();
        assertTrue(viewModel.sessions().isEmpty());
    }

    @Test
    void sessionDetail_returnsTheSessionFromTheUseCase() {
        UUID id = UUID.randomUUID();
        MeasurementSession session = new MeasurementSession(id, "cam-0", NOW);
        when(getDetailUseCase.execute(id)).thenReturn(Optional.of(session));

        assertEquals(session, viewModel.sessionDetail(id).orElseThrow());
    }
}
