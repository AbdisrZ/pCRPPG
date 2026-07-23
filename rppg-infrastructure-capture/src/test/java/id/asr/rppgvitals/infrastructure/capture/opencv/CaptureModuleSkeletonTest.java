package id.asr.rppgvitals.infrastructure.capture.opencv;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// Skeleton test accompanying the `rppg-infrastructure-capture` module's introduction (T-001),
/// per `00 §20.1`. Replaced by real adapter tests from T-201 onward.
class CaptureModuleSkeletonTest {

    @Test
    void javaRuntime_underProjectToolchain_isAtLeastJava25() {
        assertTrue(Runtime.version().feature() >= 25);
    }
}
