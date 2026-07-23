package id.asr.rppgvitals.application.usecase.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.session.SessionStatus;

class StartMeasurementSessionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-07-23T09:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void execute_createsAnActiveSessionAtTheCurrentTime() {
        MeasurementSession session = new StartMeasurementSessionUseCase(clock).execute("cam-0");

        assertEquals("cam-0", session.deviceIdentifier());
        assertEquals(NOW, session.startedAt());
        assertEquals(SessionStatus.ACTIVE, session.status());
    }

    @Test
    void execute_withNullDeviceIdentifier_throws() {
        assertThrows(NullPointerException.class, () -> new StartMeasurementSessionUseCase(clock).execute(null));
    }

    @Test
    void constructor_withNullClock_throws() {
        assertThrows(NullPointerException.class, () -> new StartMeasurementSessionUseCase(null));
    }
}
