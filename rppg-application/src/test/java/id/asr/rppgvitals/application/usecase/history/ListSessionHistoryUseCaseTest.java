package id.asr.rppgvitals.application.usecase.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.session.MeasurementRepository;
import id.asr.rppgvitals.domain.session.SessionStatus;
import id.asr.rppgvitals.domain.session.SessionSummary;

class ListSessionHistoryUseCaseTest {

    private final MeasurementRepository repository = mock(MeasurementRepository.class);

    @Test
    void execute_returnsTheRepositorySummaries() {
        SessionSummary summary = new SessionSummary(
                UUID.randomUUID(), Instant.parse("2026-07-23T09:00:00Z"), null, SessionStatus.COMPLETED, 72.0, 0.8);
        when(repository.findAllSummaries()).thenReturn(List.of(summary));

        List<SessionSummary> result = new ListSessionHistoryUseCase(repository).execute();

        assertEquals(List.of(summary), result);
    }

    @Test
    void constructor_withNullRepository_throws() {
        assertThrows(NullPointerException.class, () -> new ListSessionHistoryUseCase(null));
    }
}
