package id.asr.rppgvitals.domain.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;

class SessionSummaryTest {

    private static final Instant START = Instant.parse("2026-07-22T10:00:00Z");
    private static final Instant END = Instant.parse("2026-07-22T10:05:00Z");
    private static final String DEVICE = "cam-0";

    @Test
    void from_completedSessionWithEstimates_computesMeans() {
        MeasurementSession session = new MeasurementSession(UUID.randomUUID(), DEVICE, START);
        session.recordEstimate(new HeartRateEstimate(70.0, 0.7, START, "CHROM"));
        session.recordEstimate(new HeartRateEstimate(80.0, 0.9, START, "CHROM"));
        session.complete(END);

        SessionSummary summary = SessionSummary.from(session);

        assertEquals(session.id(), summary.sessionId());
        assertEquals(START, summary.startedAt());
        assertEquals(END, summary.endedAt());
        assertEquals(SessionStatus.COMPLETED, summary.status());
        assertEquals(75.0, summary.meanHeartRateBpm());
        assertEquals(0.8, summary.meanConfidence());
    }

    @Test
    void from_activeSessionWithNoEstimates_hasNullMeansAndNullEnd() {
        MeasurementSession session = new MeasurementSession(UUID.randomUUID(), DEVICE, START);

        SessionSummary summary = SessionSummary.from(session);

        assertNull(summary.meanHeartRateBpm());
        assertNull(summary.meanConfidence());
        assertNull(summary.endedAt());
    }

    @Test
    void from_withNullSession_throws() {
        assertThrows(NullPointerException.class, () -> SessionSummary.from(null));
    }

    @Test
    void constructor_withNullMeans_isAccepted() {
        SessionSummary summary = new SessionSummary(UUID.randomUUID(), START, END, SessionStatus.COMPLETED, null, null);

        assertNull(summary.meanHeartRateBpm());
    }

    @Test
    void constructor_withNullSessionId_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new SessionSummary(null, START, END, SessionStatus.COMPLETED, 70.0, 0.8));
    }

    @Test
    void constructor_withNullStartedAt_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new SessionSummary(UUID.randomUUID(), null, END, SessionStatus.COMPLETED, 70.0, 0.8));
    }

    @Test
    void constructor_withNullStatus_throws() {
        assertThrows(
                NullPointerException.class, () -> new SessionSummary(UUID.randomUUID(), START, END, null, 70.0, 0.8));
    }

    @Test
    void constructor_withNegativeMeanBpm_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SessionSummary(UUID.randomUUID(), START, END, SessionStatus.COMPLETED, -1.0, 0.8));
    }

    @Test
    void constructor_withNonFiniteMeanBpm_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SessionSummary(UUID.randomUUID(), START, END, SessionStatus.COMPLETED, Double.NaN, 0.8));
    }

    @Test
    void constructor_withConfidenceOutOfRange_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SessionSummary(UUID.randomUUID(), START, END, SessionStatus.COMPLETED, 70.0, 1.5));
    }
}
