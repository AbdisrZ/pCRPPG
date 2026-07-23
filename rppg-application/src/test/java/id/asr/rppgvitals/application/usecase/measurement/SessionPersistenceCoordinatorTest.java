package id.asr.rppgvitals.application.usecase.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.session.MeasurementSession;

class SessionPersistenceCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-07-24T09:00:00Z");

    private final EndMeasurementSessionUseCase endUseCase = mock(EndMeasurementSessionUseCase.class);
    // A same-thread executor makes the asynchronous dispatch deterministic under test.
    private final Executor directExecutor = Runnable::run;
    private final SessionPersistenceCoordinator coordinator =
            new SessionPersistenceCoordinator(endUseCase, directExecutor);

    private MeasurementSession newSession() {
        return new MeasurementSession(UUID.randomUUID(), "cam-0", NOW);
    }

    @Test
    void endAsync_persistsAndInvokesOnSaved() {
        MeasurementSession session = newSession();
        List<String> events = new CopyOnWriteArrayList<>();

        coordinator.endAsync(session, () -> events.add("saved"), failure -> events.add("error"));

        verify(endUseCase).execute(session);
        assertEquals(List.of("saved"), events);
    }

    @Test
    void endAsync_whenWriteFails_invokesOnErrorWithTheFailure() {
        MeasurementSession session = newSession();
        RuntimeException boom = new IllegalStateException("disk full");
        doThrow(boom).when(endUseCase).execute(session);
        List<RuntimeException> failures = new CopyOnWriteArrayList<>();

        coordinator.endAsync(session, () -> failures.add(null), failures::add);

        assertEquals(1, failures.size());
        assertSame(boom, failures.get(0));
    }

    @Test
    void endNow_persistsOnTheCallingThread() {
        MeasurementSession session = newSession();

        coordinator.endNow(session);

        verify(endUseCase).execute(session);
    }

    @Test
    void constructor_rejectsNullCollaborators() {
        assertThrows(NullPointerException.class, () -> new SessionPersistenceCoordinator(null, directExecutor));
        assertThrows(NullPointerException.class, () -> new SessionPersistenceCoordinator(endUseCase, null));
    }

    @Test
    void endAsync_rejectsNullArguments() {
        MeasurementSession session = newSession();
        assertThrows(NullPointerException.class, () -> coordinator.endAsync(null, () -> {}, failure -> {}));
        assertThrows(NullPointerException.class, () -> coordinator.endAsync(session, null, failure -> {}));
        assertThrows(NullPointerException.class, () -> coordinator.endAsync(session, () -> {}, null));
    }
}
