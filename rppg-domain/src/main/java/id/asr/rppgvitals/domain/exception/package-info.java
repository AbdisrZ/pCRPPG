/// The project's sealed exception hierarchy.
///
/// Roots the sealed `RppgApplicationException` tree (`00 §22.2`) whose permitted subtypes —
/// `CameraUnavailableException`, `SignalQualityException`, `ModelInferenceException`,
/// `PersistenceException`, `ConfigurationException` — enable exhaustive pattern-matched
/// handling at the presentation boundary. Infrastructure checked exceptions are translated
/// into these unchecked types at the adapter boundary (`00 §22.1`); no subtype may reside
/// outside this package (`04 §8` Sealed Exception Closure rule).
package id.asr.rppgvitals.domain.exception;
