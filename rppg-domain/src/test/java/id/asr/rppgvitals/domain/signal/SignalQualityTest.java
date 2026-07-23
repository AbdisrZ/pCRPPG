package id.asr.rppgvitals.domain.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.signal.SignalQuality.Degraded;
import id.asr.rppgvitals.domain.signal.SignalQuality.Searching;
import id.asr.rppgvitals.domain.signal.SignalQuality.Stable;

class SignalQualityTest {

    @Test
    void searchingAndStable_areValueEqualByState() {
        assertEquals(new Searching(), new Searching());
        assertEquals(new Stable(), new Stable());
        assertNotEquals(new Searching(), new Stable());
    }

    @Test
    void degraded_retainsReason() {
        Degraded degraded = new Degraded("camera disconnected");

        assertEquals("camera disconnected", degraded.reason());
    }

    @Test
    void degraded_isPatternMatchable() {
        SignalQuality quality = new Degraded("insufficient lighting");

        String description =
                switch (quality) {
                    case Searching ignored -> "searching";
                    case Stable ignored -> "stable";
                    case Degraded d -> d.reason();
                };

        assertEquals("insufficient lighting", description);
    }

    @Test
    void degraded_withNullReason_throws() {
        assertThrows(NullPointerException.class, () -> new Degraded(null));
    }

    @Test
    void degraded_withBlankReason_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Degraded("  "));
    }
}
