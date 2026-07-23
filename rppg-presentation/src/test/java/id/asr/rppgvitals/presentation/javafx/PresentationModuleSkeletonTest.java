package id.asr.rppgvitals.presentation.javafx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// Skeleton test accompanying the `rppg-presentation` module's introduction (T-001), per
/// `00 §20.1`. Replaced by real ViewModel tests from T-401 onward.
class PresentationModuleSkeletonTest {

    @Test
    void javaRuntime_underProjectToolchain_isAtLeastJava25() {
        assertTrue(Runtime.version().feature() >= 25);
    }
}
