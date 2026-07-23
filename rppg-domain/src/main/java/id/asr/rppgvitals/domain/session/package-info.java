/// The measurement-session entity, its projections, and its persistence contract.
///
/// Holds the `MeasurementSession` entity (the domain's only identity-bearing type), the
/// `SessionSummary` history projection, and the [MeasurementRepository] port. Per `00 §21.1`
/// the repository is kept narrow — persist, list, retrieve, delete — with the SQLite adapter
/// in `infrastructure.persistence.sqlite` depending inward on it. Governed by
/// `03_ARCHITECTURE.md §3–§4` and `10_DATABASE.md`.
package id.asr.rppgvitals.domain.session;
