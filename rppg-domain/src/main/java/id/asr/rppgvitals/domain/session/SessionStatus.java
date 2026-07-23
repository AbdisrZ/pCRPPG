package id.asr.rppgvitals.domain.session;

/// The lifecycle status of a [MeasurementSession] (`03_ARCHITECTURE.md §3`, realising the terminal
/// states of the session model in `02_SOFTWARE_REQUIREMENT.md §3.4`).
public enum SessionStatus {

    /// The session is in progress and still accumulating estimates.
    ACTIVE,

    /// The session ended normally at the user's request.
    COMPLETED,

    /// The session ended abnormally, for example after an unrecoverable capture failure.
    ABORTED
}
