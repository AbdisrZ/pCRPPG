/// Application composition root and process entry point.
///
/// The one package that is not part of any single architectural layer (`04 §2`): `Main` and
/// `CompositionRoot` assemble the concrete adapters behind their domain ports, own the executor
/// and connection lifecycle, and sequence deterministic shutdown (`11 §8`). Singletons are
/// provided here by explicit injection, never via static `getInstance()` (`00 §30`). This module
/// also hosts the ArchUnit fitness suite (`04 §8`). Governed by `03_ARCHITECTURE.md §8`.
package id.asr.rppgvitals.bootstrap;
