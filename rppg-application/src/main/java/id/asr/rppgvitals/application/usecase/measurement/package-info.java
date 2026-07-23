/// Application use cases that orchestrate a live measurement session.
///
/// Holds `StartMeasurementSessionUseCase`, `EndMeasurementSessionUseCase`, the
/// `LiveMeasurementOrchestrator`, and the `MeasurementObserver` callback contract, plus the
/// single Scoped-Value declaration for the session correlation id (`00 §15`, `05 §6`). Use
/// cases orchestrate, they do not compute — signal math stays in `domain.estimation` (`00 §9`).
/// Depends only on `rppg-domain`; never on `javafx.*` (`04 §8` Application Purity rule).
/// Governed by `03_ARCHITECTURE.md §6.2` and `11_THREADING.md`.
package id.asr.rppgvitals.application.usecase.measurement;
