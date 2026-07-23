package id.asr.rppgvitals.domain.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.signal.SignalQuality.Degraded;
import id.asr.rppgvitals.domain.signal.SignalQuality.Searching;
import id.asr.rppgvitals.domain.signal.SignalQuality.Stable;

class SignalQualityStateMachineTest {

    private final SignalQualityStateMachine machine = new SignalQualityStateMachine();

    @Test
    void initial_isSearching() {
        assertInstanceOf(Searching.class, machine.initial());
    }

    @Test
    void afterEstimate_withConfidenceAtThreshold_becomesStable() {
        SignalQuality next =
                machine.afterEstimate(new Searching(), SignalQualityStateMachine.STABILITY_THRESHOLD, true);

        assertInstanceOf(Stable.class, next);
    }

    @Test
    void afterEstimate_withConfidenceBelowThreshold_becomesSearching() {
        SignalQuality next = machine.afterEstimate(new Stable(), 0.29, true);

        assertInstanceOf(Searching.class, next);
    }

    @Test
    void afterEstimate_withIncompleteWindow_staysSearching() {
        SignalQuality next = machine.afterEstimate(new Searching(), 0.95, false);

        assertInstanceOf(Searching.class, next);
    }

    @Test
    void afterEstimate_whenDegraded_staysDegraded() {
        SignalQuality next = machine.afterEstimate(new Degraded("camera lost"), 0.95, true);

        assertInstanceOf(Degraded.class, next);
    }

    @Test
    void afterDegradingCondition_becomesDegradedWithReason() {
        SignalQuality next = machine.afterDegradingCondition("insufficient lighting");

        assertEquals(
                "insufficient lighting", assertInstanceOf(Degraded.class, next).reason());
    }

    @Test
    void afterConditionResolved_fromDegraded_becomesSearching() {
        SignalQuality next = machine.afterConditionResolved(new Degraded("camera lost"));

        assertInstanceOf(Searching.class, next);
    }

    @Test
    void afterConditionResolved_whenNotDegraded_isUnchanged() {
        SignalQuality current = new Stable();

        assertEquals(current, machine.afterConditionResolved(current));
    }

    @Test
    void transitions_rejectNullState() {
        assertThrows(NullPointerException.class, () -> machine.afterEstimate(null, 0.5, true));
        assertThrows(NullPointerException.class, () -> machine.afterConditionResolved(null));
    }
}
