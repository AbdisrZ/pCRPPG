# ADR 0003 — Domain Core Modeling and Phase-1 Gate Refinements

- **Status:** Accepted
- **Date:** 2026-07-22
- **Decision process:** `00_MASTER_PROMPT.md §8`, node G (reversible, low-risk) for the modeling
  choices; completes/supersedes parts of ADR 0002 (§5 doclint, §6 coverage aggregate).
- **Implements:** Phase 1 tasks T-101 (value objects + `MeasurementSession`), T-102 (sealed
  exception hierarchy), T-103 (the four domain ports).

## Context

`03_ARCHITECTURE.md §3` deliberately defers "exact field types, validation rules, and JavaDoc" to
implementation, and `§4` gives port responsibilities and illustrative method names but not full
signatures. This ADR records the concrete choices made for the domain core, and two build-gate
refinements that Phase-1 code forced to resolution. Everything below is validated by a green
`./mvnw -P ci clean verify` on JDK 25 (80 domain unit tests; domain coverage ≥ 90% line / 85% branch).

## Domain-modeling decisions

1. **`Frame` defensively copies its pixel buffer** (in the compact constructor and the `pixels()`
   accessor), making it a genuinely immutable value object per `05 §6`. This is the correctness-first
   default (`00 §5`); SpotBugs' `EI_EXPOSE_REP` confirmed the alternative (sharing the array) leaks
   state. Whether the per-frame copy costs too much against the `00 §11` budget is a question for
   measurement, not assumption (`00 §32`) — any zero-copy optimisation is deferred to the Phase-5
   latency work and, if adopted, introduced behind this same type with its own ADR.
2. **`SignalQuality` is a sealed interface with data-carrying variants** (`Searching`, `Stable`,
   `Degraded(String reason)`), not an `enum`. `Degraded` carries the reason surfaced through the
   `onSessionDegraded(reason)` observer callback (`03 §6.2`), and the sealed shape enables the
   exhaustive, `default`-free `switch` at the presentation boundary (`05 §6`). Matches the
   `SignalQuality.Stable` / `.Degraded` naming in `05 §4`. Transition rules remain T-108's concern.
3. **`SessionStatus` enum** (`ACTIVE` / `COMPLETED` / `ABORTED`) realises the `MeasurementSession`
   "final status" of `03 §3`, mapping to the terminal states of the `02 §3.4` session model. It is a
   supporting implementation detail of the entity's status field, not a new architectural component.
4. **Port result shapes.** `InferenceEngine.detectRegionOfInterest` returns `Optional<RegionOfInterest>`
   — the "no face" case is an empty `Optional`, never an exception (`00 §22.2`, `05 §7`).
   `MeasurementRepository.findById` returns `Optional<MeasurementSession>`. `FrameSource` extends
   `AutoCloseable` with a non-throwing `close()`. Thrown domain exceptions are documented via `@throws`.
5. **`MeasurementSession` is the sole Entity** — an explicit final class with identity-based
   `equals`/`hashCode` on its `UUID` and single-writer confinement (documented, `00 §25`), rather than
   a record, because it has mutable state that evolves over the session's life (`05 §6`).

## Phase-0 gate refinements forced by real code

6. **JavaDoc-presence gate wired via javac `-Xdoclint:all/protected` (resolves NC-004).** Verified
   that doclint natively understands JEP 467 `///` Markdown JavaDoc: documented types/records/enums/
   sealed interfaces pass, a missing `@param` or a missing comment fails under `-Werror`. It is added
   to the `ci` compiler profile alongside `-Xlint:all -Werror`, completing ADR 0002 §5's deferred
   plan. This is the mechanism NC-004 asked to confirm.
7. **Checkstyle `UnusedImports`/`RedundantImport` removed; Spotless owns unused-import removal.**
   Checkstyle's `UnusedImports` (like its JavaDoc checks) does not see types referenced only from a
   `///` comment, so it false-flagged every javadoc-only import (e.g. a documented `@throws` type).
   Spotless's `removeUnusedImports` is `///`-aware (verified: it keeps javadoc-referenced imports and
   still fails a genuinely unused one). Checkstyle keeps `AvoidStarImport`.
8. **Coverage: per-module 80%-line floor replaces the cross-module aggregate check** (supersedes
   ADR 0002 §6). JaCoCo's `check` goal cannot span modules — the previous `merge`+`check` in `rppg-app`
   only ever analysed `rppg-app`'s own (empty) classes, so it enforced nothing. Every module now
   inherits an 80%-line floor; enforcing it per module is stricter than, and therefore implies,
   `13 §7`'s project-wide ≥ 80% line aggregate. The domain adds the stricter 90/85 rule.
   `report-aggregate` in `rppg-app` provides the true combined coverage report for visibility.

## Consequences

- The domain core (value objects, sealed exceptions, four ports) exists, is fully `///`-documented,
  and is covered ≥ 90/85 by 80 unit tests; `./mvnw -P ci clean verify` is green.
- NC-004 is resolved (interim, pending architect ratification): doclint is the JavaDoc-presence gate.
- All gates are proven live: a missing `@param` fails the `ci` build; an unused import fails Spotless;
  a non-defensive array copy fails SpotBugs.
- The `SessionStatus` enum is a type not enumerated in `04 §4`'s tree; if the architect prefers it
  modelled differently, the change is mechanical.
