/// Application use cases for browsing and managing stored measurement sessions.
///
/// Holds `ListSessionHistoryUseCase`, `GetSessionDetailUseCase`, and `DeleteSessionUseCase`,
/// each a single-intent use case mediating access through the domain `MeasurementRepository`
/// port (`00 §30` Repository pattern). Depends only on `rppg-domain`; never on `javafx.*`.
/// Governed by `03_ARCHITECTURE.md §6.2` and `02_SOFTWARE_REQUIREMENT.md` (FR-202).
package id.asr.rppgvitals.application.usecase.history;
