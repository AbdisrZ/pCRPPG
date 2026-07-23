package id.asr.rppgvitals.domain.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;

class MeasurementSessionTest {

    private static final Instant START = Instant.parse("2026-07-22T10:00:00Z");
    private static final Instant END = Instant.parse("2026-07-22T10:05:00Z");
    private static final String DEVICE = "cam-0";

    private static MeasurementSession newSession() {
        return new MeasurementSession(UUID.randomUUID(), DEVICE, START);
    }

    private static HeartRateEstimate estimate(double bpm) {
        return new HeartRateEstimate(bpm, 0.8, START, "CHROM");
    }

    @Test
    void newSession_isActiveWithNoEndAndNoEstimates() {
        MeasurementSession session = newSession();

        assertEquals(SessionStatus.ACTIVE, session.status());
        assertTrue(session.endedAt().isEmpty());
        assertTrue(session.estimates().isEmpty());
        assertTrue(session.latestEstimate().isEmpty());
        assertEquals(DEVICE, session.deviceIdentifier());
        assertEquals(START, session.startedAt());
    }

    @Test
    void recordEstimate_appendsInOrderAndTracksLatest() {
        MeasurementSession session = newSession();

        session.recordEstimate(estimate(70.0));
        session.recordEstimate(estimate(72.0));

        assertEquals(2, session.estimates().size());
        assertEquals(72.0, session.latestEstimate().orElseThrow().beatsPerMinute());
    }

    @Test
    void complete_setsCompletedStatusAndEndInstant() {
        MeasurementSession session = newSession();

        session.complete(END);

        assertEquals(SessionStatus.COMPLETED, session.status());
        assertEquals(END, session.endedAt().orElseThrow());
    }

    @Test
    void abort_setsAbortedStatus() {
        MeasurementSession session = newSession();

        session.abort(END);

        assertEquals(SessionStatus.ABORTED, session.status());
    }

    @Test
    void recordEstimate_afterEnd_throws() {
        MeasurementSession session = newSession();
        session.complete(END);

        assertThrows(IllegalStateException.class, () -> session.recordEstimate(estimate(70.0)));
    }

    @Test
    void complete_afterEnd_throws() {
        MeasurementSession session = newSession();
        session.complete(END);

        assertThrows(IllegalStateException.class, () -> session.complete(END));
    }

    @Test
    void complete_withEndBeforeStart_throws() {
        MeasurementSession session = newSession();

        assertThrows(IllegalArgumentException.class, () -> session.complete(START.minusSeconds(1)));
    }

    @Test
    void constructor_withNullArgument_throws() {
        assertThrows(NullPointerException.class, () -> new MeasurementSession(null, DEVICE, START));
        assertThrows(NullPointerException.class, () -> new MeasurementSession(UUID.randomUUID(), null, START));
        assertThrows(NullPointerException.class, () -> new MeasurementSession(UUID.randomUUID(), DEVICE, null));
    }

    @Test
    void constructor_withBlankDeviceIdentifier_throws() {
        assertThrows(IllegalArgumentException.class, () -> new MeasurementSession(UUID.randomUUID(), "  ", START));
    }

    @Test
    void recordEstimate_withNull_throws() {
        MeasurementSession session = newSession();

        assertThrows(NullPointerException.class, () -> session.recordEstimate(null));
    }

    @Test
    void complete_withNullInstant_throws() {
        MeasurementSession session = newSession();

        assertThrows(NullPointerException.class, () -> session.complete(null));
    }

    @Test
    void estimates_returnsImmutableSnapshot() {
        MeasurementSession session = newSession();
        session.recordEstimate(estimate(70.0));

        assertThrows(
                UnsupportedOperationException.class, () -> session.estimates().clear());
    }

    @Test
    void equality_isByIdentityOnly() {
        UUID id = UUID.randomUUID();
        MeasurementSession a = new MeasurementSession(id, DEVICE, START);
        MeasurementSession b = new MeasurementSession(id, DEVICE, END);
        MeasurementSession other = newSession();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, other);
        assertNotEquals(a, new Object());
    }

    @Test
    void toString_includesIdentityAndStatusButNoSignalData() {
        MeasurementSession session = newSession();
        session.recordEstimate(estimate(70.0));

        String text = session.toString();

        assertTrue(text.contains(session.id().toString()));
        assertTrue(text.contains("ACTIVE"));
        assertFalse(text.contains("70.0"));
    }
}
