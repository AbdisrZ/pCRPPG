# ADR 0009 — Presentation ViewModel Foundation (Phase 4)

- **Status:** Accepted
- **Date:** 2026-07-23
- **Decision process:** `00_MASTER_PROMPT.md §8`, node G.
- **Implements:** the ViewModel core of T-402 (Live Measurement) and T-403 (Session History), and the
  marshaling convention of T-405. The FXML views, Controllers, disclosure (T-401), detail + XChart
  (T-404), and the JavaFX `Application` launcher remain — they need a display to verify and are
  covered by the manual UI checklist (`13 §4`), not automated tests.

## Context

Phase 4 is JavaFX and largely un-unit-testable at the View level. To land verifiable, safety-critical
value first, the logic was concentrated in ViewModels (which are testable headless) and separated from
the JavaFX views. Validated by a green `./mvnw -P ci clean verify` (15 presentation tests).

## Decisions

1. **JavaFX 25.0.4 with platform classifiers.** Each JavaFX module (`base`, `graphics`, `controls`,
   `fxml`) is declared explicitly in `rppg-presentation` with `<classifier>${javafx.platform}</classifier>`,
   because Maven does not propagate classifiers transitively. `javafx.platform` comes from the T-004
   profiles. XChart 4.0.3 is added for the (later) T-404 trend chart.
2. **`ConfidenceTier` (shared display logic).** An enum mapping a confidence score to a display tier
   (High ≥ 0.8, Moderate 0.5–0.79, Low < 0.5) with colour and guidance message (`06 §3`, `§7`). Shared
   by the live and history screens so the tiering is defined once (`06 §6.3`). Placed in
   `presentation.javafx.dashboard` (with the other confidence-indicator logic); `history` imports it,
   which keeps every presentation class inside a `04 §4` leaf package (the ArchUnit package rule).
3. **`UiThreadExecutor` marshaling abstraction.** The `MeasurementObserver` callbacks arrive on the
   processing thread and must reach the FX thread via `Platform.runLater` (`11 §9`). Abstracting that
   single hand-off behind a functional interface makes `LiveMeasurementViewModel` unit-testable with a
   synchronous executor and no running toolkit — the production `Platform.runLater` implementation
   lands with the launcher (T-405). JavaFX `Property`/`ObservableList` classes themselves need no
   toolkit, so the ViewModels are fully testable headless.
4. **ViewModels are the presentation-side coordinators.** `LiveMeasurementViewModel` implements
   `MeasurementObserver`, exposes `Property` state named for meaning (`06 §8`), realises the
   state-to-treatment table of `06 §6.2` (a number and tier only while `STABLE`; a guidance message
   while searching/degraded), and drives the Start/End use cases and the orchestrator.
   `SessionHistoryViewModel` exposes the session list and delegates list/delete/detail. History
   persistence is currently synchronous on the calling thread; moving it onto a `persistenceExecutor`
   (`11 §2`) is deferred to T-501.
5. **One recorded SpotBugs suppression** (`config/spotbugs/exclude.xml`): `EI_EXPOSE_REP`/`REP2` on the
   presentation `*ViewModel` getters. Exposing the live JavaFX `Property`/`ObservableList` is the MVVM
   binding contract (`06 §8`); a defensive copy would break binding. SpotBugs does not model JavaFX
   observables. Scoped narrowly to the ViewModels.

## Consequences

- The safety-critical confidence-tier logic and the live/history state behaviour exist and are unit
  tested; the remaining Phase-4 work is the JavaFX views/controllers/launcher, verifiable through the
  manual UI checklist and by running the app.
- The camera preview + ROI overlay of `06 §6.2` is **not** delivered by the current `MeasurementObserver`
  contract (which carries estimates/quality, not frames/ROI). Adding it needs an observer extension or a
  separate frame tap — recorded as a follow-up, not built here.
