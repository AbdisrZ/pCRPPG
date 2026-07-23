package id.asr.rppgvitals.application.usecase.measurement;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import id.asr.rppgvitals.domain.session.MeasurementSession;

/// Starts a new measurement session (`03_ARCHITECTURE.md §6.2`).
///
/// This use case owns the creation of the [MeasurementSession] entity — assigning its domain-layer
/// identity and start time (`10_DATABASE.md §4`). Driving the live capture/estimation pipeline for
/// the session is the `LiveMeasurementOrchestrator`'s responsibility (T-302), which this use case
/// hands the freshly created session to.
public final class StartMeasurementSessionUseCase {

    private final Clock clock;

    /// Creates the use case.
    ///
    /// @param clock the clock supplying the session start time; never `null`
    public StartMeasurementSessionUseCase(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /// Creates a new active session against the given device.
    ///
    /// @param deviceIdentifier the identifier of the camera device to run against; never `null` or blank
    /// @return the newly created, active session with a fresh identity and the current start time
    public MeasurementSession execute(String deviceIdentifier) {
        Objects.requireNonNull(deviceIdentifier, "deviceIdentifier");
        return new MeasurementSession(UUID.randomUUID(), deviceIdentifier, clock.instant());
    }
}
