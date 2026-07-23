package id.asr.rppgvitals.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// Skeleton test accompanying the `rppg-app` module's introduction (T-001), per `00 §20.1`.
/// This module additionally hosts the ArchUnit fitness tests once T-002 lands.
class BootstrapModuleSkeletonTest {

    @Test
    void javaRuntime_underProjectToolchain_isAtLeastJava25() {
        assertTrue(Runtime.version().feature() >= 25);
    }
}
