package id.asr.rppgvitals.application.usecase.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.session.MeasurementRepository;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.session.SessionStatus;

class EndMeasurementSessionUseCaseTest {

    private static final Instant START = Instant.parse("2026-07-23T09:00:00Z");
    private static final Instant END = Instant.parse("2026-07-23T09:05:00Z");

    private final MeasurementRepository repository = mock(MeasurementRepository.class);
    private final Clock clock = Clock.fixed(END, ZoneOffset.UTC);

    private MeasurementSession activeSession() {
        return new MeasurementSession(UUID.randomUUID(), "cam-0", START);
    }

    @Test
    void execute_completesTheSessionAndPersistsIt() {
        MeasurementSession session = activeSession();

        new EndMeasurementSessionUseCase(repository, clock).execute(session);

        assertEquals(SessionStatus.COMPLETED, session.status());
        assertEquals(END, session.endedAt().orElseThrow());
        verify(repository).save(session);
    }

    @Test
    void execute_onAlreadyEndedSession_throws() {
        MeasurementSession session = activeSession();
        session.complete(END);
        EndMeasurementSessionUseCase useCase = new EndMeasurementSessionUseCase(repository, clock);

        assertThrows(IllegalStateException.class, () -> useCase.execute(session));
    }

    @Test
    void execute_withNullSession_throws() {
        EndMeasurementSessionUseCase useCase = new EndMeasurementSessionUseCase(repository, clock);

        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void constructor_withNullArguments_throws() {
        assertThrows(NullPointerException.class, () -> new EndMeasurementSessionUseCase(null, clock));
        assertThrows(NullPointerException.class, () -> new EndMeasurementSessionUseCase(repository, null));
    }
}
