package id.asr.rppgvitals.application.usecase.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.session.MeasurementRepository;
import id.asr.rppgvitals.domain.session.MeasurementSession;

class GetSessionDetailUseCaseTest {

    private final MeasurementRepository repository = mock(MeasurementRepository.class);

    @Test
    void execute_returnsTheSessionFromTheRepository() {
        UUID id = UUID.randomUUID();
        MeasurementSession session = new MeasurementSession(id, "cam-0", Instant.parse("2026-07-23T09:00:00Z"));
        when(repository.findById(id)).thenReturn(Optional.of(session));

        Optional<MeasurementSession> result = new GetSessionDetailUseCase(repository).execute(id);

        assertEquals(session, result.orElseThrow());
    }

    @Test
    void execute_withUnknownId_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertTrue(new GetSessionDetailUseCase(repository).execute(id).isEmpty());
    }

    @Test
    void execute_withNullId_throws() {
        assertThrows(NullPointerException.class, () -> new GetSessionDetailUseCase(repository).execute(null));
    }

    @Test
    void constructor_withNullRepository_throws() {
        assertThrows(NullPointerException.class, () -> new GetSessionDetailUseCase(null));
    }
}
