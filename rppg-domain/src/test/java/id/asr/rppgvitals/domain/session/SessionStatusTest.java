package id.asr.rppgvitals.domain.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SessionStatusTest {

    @Test
    void values_containsTheThreeLifecycleStates() {
        assertEquals(3, SessionStatus.values().length);
        assertEquals(SessionStatus.ACTIVE, SessionStatus.valueOf("ACTIVE"));
        assertEquals(SessionStatus.COMPLETED, SessionStatus.valueOf("COMPLETED"));
        assertEquals(SessionStatus.ABORTED, SessionStatus.valueOf("ABORTED"));
    }
}
