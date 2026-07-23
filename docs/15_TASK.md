# 15_TASK.md
# Task Backlog
## rPPG Desktop Vitals Monitor

---

**Document Control**

| Field | Value |
|---|---|
| Document ID | TSK-15 |
| Version | 1.0.0 (seed) |
| Status | **LIVING** — unlike every other document in this set, this one is expected to change constantly and is never "ratified" in the same sense. |
| Depends On | Every document in this set, 00 through 14 |
| Consumed By | Nothing — this is the terminal document; work happens against it, not against a document downstream of it. |
| Precedence | Subordinate to all of `00`–`14`. A task here that contradicts any of them is a defect in this document, not a license to deviate from them. |
| Maintainer | Human Project Architect — Abdi Soleh Rosadi |
| Last Updated | 2026-07-22 |

---

## 1. Purpose and Usage of This Document

Every other document in this set (`00`–`14`) is governance: stable, deliberately slow-changing, ratified. This one is the opposite by design — it is where governance turns into work. `00 §37`'s Incremental Development Protocol, `00 §40`'s Escalation Protocol, and every "traces to `15_TASK.md`" reference scattered across the other fourteen documents all point here.

**How an agent uses this document:** pick the next `Not Started` task whose dependencies are `Done`, read the documents it traces to, do the work, satisfy `00 §19.1`'s Definition of Done, mark it `Done`, and update this file in the same change — this file going stale relative to actual repository state is itself a defect (`00 §17`).

**How this document grows:** as phases 0–2 are completed, phases 3–6 below (currently coarse-grained, deliberately) are broken down into the same level of detail phases 0–1 already have, per `00 §37`'s "the port is introduced first" sequencing — this document does not pretend to have fully planned work seven layers of architecture away from what's being built today.

---

## 2. Task Notation and Status Values

- **ID scheme:** `T-0xx` through `T-6xx`, grouped by phase (§3–§9), mirroring the `FR-1xx`/`NFR-1xx` grouping convention established in `02`.
- **Status:** `Not Started` / `In Progress` / `Blocked` / `Needs Clarification` / `Done`.
- **Traces To:** every task cites the specific document section it implements — a task with no citation is not well-formed and should not be started until it has one.
- A `Blocked` task names what it is blocked on (another task ID, or an entry in §10).

---

## 3. Phase 0 — Project Scaffolding

| ID | Task | Traces To | Status |
|---|---|---|---|
| T-001 | Initialize the seven-module Maven project structure. | `03 §8` | Done |
| T-002 | Configure Spotless (Palantir Java Format), Checkstyle, PMD, SpotBugs, and ArchUnit in the build. | `05 §13`, `04 §8` | Done — see ADR 0002; JavaDoc-presence sub-rule reassigned to javac doclint (NC-004). |
| T-003 | Stand up the every-commit CI pipeline skeleton. | `13 §5` | Done — `.github/workflows/ci.yml`; ready-to-run, no git remote configured yet. |
| T-004 | Configure Maven platform profiles for native-library classifiers (Windows/macOS/Linux × x86-64/ARM64). | `00 §26`, `14 §3` | Done — four platform profiles + build-time unsupported-platform guard (ADR 0002). |
| T-005 | Configure JaCoCo coverage reporting and thresholds. | `13 §7` | Done — domain 90/85 + per-module 80%-line floor (implies the project aggregate; ADR 0003 §8 supersedes ADR 0002 §6) + `report-aggregate`. |
| T-006 | Create `package-info.java` stubs for every package in `04 §4`'s tree. | `04 §5` | Done — 15 leaf-package stubs (ADR 0002 §8). |

---

## 4. Phase 1 — Domain Core

| ID | Task | Traces To | Status |
|---|---|---|---|
| T-101 | Implement the domain value objects and the `MeasurementSession` entity. | `03 §3` | Done — see ADR 0003; 80 domain unit tests, coverage ≥ 90/85. |
| T-102 | Implement the sealed `RppgApplicationException` hierarchy. | `00 §22.2`, `04 §4` | Done — root + five subtypes, each carrying actionable context (ADR 0003). |
| T-103 | Define the four domain ports: `FrameSource`, `InferenceEngine`, `SignalEstimator`, `MeasurementRepository`. | `03 §4` | Done — signatures recorded in ADR 0003. |
| T-104 | Implement `GreenChannelSignalEstimator`. | `07 §6` | Done — ADR 0004. |
| T-105 | Implement `ChromSignalEstimator`. | `07 §7` | Done — V1 default (ADR 0004). |
| T-106 | Implement `PosSignalEstimator`. | `07 §8` | Done — ADR 0004. |
| T-107 | Implement zero-padded FFT frequency estimation and the three-component confidence score. | `08 §2`, `08 §3` | Done for FFT + spectral-SNR component (ADR 0004 §5); temporal-consistency + ROI-stability components and NC-002 weights composed at the orchestrator (T-302). |
| T-108 | Implement `SignalQuality` state transition logic. | `08 §4` | Done — `SignalQualityStateMachine` (pure transitions; DEGRADED driven by orchestrator events per `03 §7.2`). |
| T-109 | Acquire golden-file fixtures and build the initial signal-processing test suite. | `13 §3` | Not Started, depends on T-104–T-106 |

T-104, T-105, and T-106 are independent of one another and may be worked in parallel or in any order.

---

## 5. Phase 2 — Infrastructure Adapters

| ID | Task | Traces To | Status |
|---|---|---|---|
| T-201 | Implement `OpenCvFrameSource`. | `03 §6.3` | Not Started, depends on T-103 |
| T-202 | Select and validate an ONNX face/landmark model against `09 §3`'s criteria. | `09 §3` | In Progress — model family selected (MediaPipe BlazeFace + Face Mesh via ONNX; ADR 0006). Empirical validation (exact artifact pin, license, reference-image check, latency) pending network + T-203. |
| T-203 | Implement `OnnxInferenceEngine`, including ROI derivation from landmarks. | `09 §5`, `09 §6` | Not Started, depends on T-202 |
| T-204 | Implement the SQLite schema DDL and the lightweight migration runner. | `10 §3`, `10 §5` | Done — `001_initial_schema.sql` + `SchemaMigrationRunner` (ADR 0005). |
| T-205 | Implement `SqliteMeasurementRepository`. | `10 §9` | Done — 10 integration tests against a temp DB; domain reconciled to the binding schema (ADR 0005). |

---

## 6. Phase 3 — Application Layer

| ID | Task | Traces To | Status |
|---|---|---|---|
| T-301 | Implement the discrete use cases (`Start`/`EndMeasurementSessionUseCase`, `List`/`Get`/`DeleteSessionUseCase` family, `ListAvailableCameraDevicesUseCase`). | `03 §6.2` | Done — six use cases, Mockito-tested at the port boundary. Start/End own session creation/finalisation+persistence; their live-pipeline wiring is T-302. |
| T-302 | Implement `LiveMeasurementOrchestrator` and the `MeasurementObserver` callback contract. | `03 §6.2`, `11` | Done — threaded orchestrator + observer + `MeasurementPipeline`; 3 deterministic concurrency tests (ADR 0007, 0008). |
| T-303 | Implement the executor topology and the capture→processing bounded queue. | `11 §2`, `11 §4` | Done for capture+processing executors + depth-3 drop-oldest queue (ADR 0008). `persistenceExecutor` (async persistence) deferred to composition root T-501. |
| T-304 | Implement the per-executor Scoped Value correlation-ID binding. | `11 §6` | Done — `CORRELATION_ID` bound per loop (ADR 0008 §5). |

Further breakdown of this phase happens once Phase 2 is `Done`, per §1's growth policy.

---

## 7. Phase 4 — Presentation Layer

| ID | Task | Traces To | Status |
|---|---|---|---|
| T-401 | Build the First-Launch Disclosure screen. | `06 §6.1` | Not Started |
| T-402 | Build the Live Measurement screen, covering every state in `06 §6.2`'s table. | `06 §6.2` | In Progress — `LiveMeasurementViewModel` (state table, observer marshaling) + `ConfidenceTier` done and tested (ADR 0009). FXML view + Controller + camera preview remain. |
| T-403 | Build the Session History screen. | `06 §6.3` | In Progress — `SessionHistoryViewModel` done and tested (ADR 0009). FXML list view + Controller + delete-confirm remain. |
| T-404 | Build the Session Detail screen, including XChart trend rendering. | `06 §6.4` | Not Started, depends on T-301 |
| T-405 | Implement ViewModel-to-`Property` binding and the `Platform.runLater` marshaling convention across all screens. | `03 §6.4`, `11 §9` | In Progress — `UiThreadExecutor` marshaling abstraction established (ADR 0009 §3); FXML bindings + the `Platform.runLater` implementation land with the launcher. |

Further breakdown happens once Phase 3 is `Done`.

---

## 8. Phase 5 — Integration and Hardening

| ID | Task | Traces To | Status |
|---|---|---|---|
| T-501 | Composition root wiring (`rppg-app`): dependency assembly, executor and connection shutdown sequencing. | `03 §8`, `11 §8` | Not Started, depends on T-401–T-405 |
| T-502 | Build the fault-injection and security/privacy assertion test suites. | `13 §2` | Not Started, depends on T-501 |
| T-503 | Build the JMH microbenchmark suite and record the first baseline. | `12 §8` | Not Started, depends on T-501 |
| T-504 | Instrument and validate end-to-end latency against `12 §4`'s budget. | `12 §4` | Not Started, depends on T-501 |
| T-505 | Measure and, if necessary, optimize cold-start time — flagged high-risk by `12 §6`, schedule this early within the phase, not last. | `12 §6` | Not Started, depends on T-501 |
| T-506 | Author the manual UI checklist document. | `13 §4` | Not Started, depends on T-402–T-404 |

---

## 9. Phase 6 — Release

| ID | Task | Traces To | Status |
|---|---|---|---|
| T-601 | Configure `jlink`/`jpackage` builds for all three target platforms. | `14 §2`, `14 §3` | Not Started, depends on T-004 |
| T-602 | Run the first packaging dry run and smoke test on all three platforms. | `14 §4`, `14 §8` | Not Started, depends on T-502–T-506, T-601 |
| T-603 | Cut the v1.0.0 release. | `14 §8` | Not Started, depends on T-602 |

---

## 10. Needs Clarification

Per `00 §40`, open questions this document set deliberately deferred rather than guessed at:

| ID | Question | Deferred By | Blocks |
|---|---|---|---|
| NC-001 | Which specific ONNX face/landmark model artifact (source, license, exact provenance) is adopted? | `09 §3` | Direction resolved 2026-07-23 (MediaPipe BlazeFace + Face Mesh via ONNX; ADR 0006). Exact artifact + license pin still open, to fix at T-202 validation. |
| NC-002 | What are the exact weights for the three confidence-score components (spectral SNR, temporal consistency, ROI stability)? | `08 §3` | The full 3-component confidence composition at the orchestrator (T-302); the estimator's spectral-SNR component (T-107) is already implemented (ADR 0004 §5). |
| NC-003 | Are the indicative hex colors in `06 §7` acceptable as final, or do they need visual-design refinement? | `06 §7` | T-402–T-404's final polish, not their initial construction |
| NC-004 | `05 §13` assigns JavaDoc-presence enforcement to Checkstyle, but no Checkstyle version (through 13.8.0) recognises the `///` Markdown JavaDoc that `05 §8` mandates. **Resolved (interim, ADR 0003 §6):** enforced via javac `-Xdoclint:all/protected` in the `ci` profile, which understands `///` natively (verified in-build). Awaiting architect ratification of the Checkstyle→doclint reassignment. | `05 §8`, `05 §13` | Nothing — resolved; open only for ratification |

---

## 11. Phase Dependency Overview

```mermaid
flowchart LR
    P0[Phase 0\nScaffolding] --> P1[Phase 1\nDomain Core]
    P1 --> P2[Phase 2\nInfrastructure]
    P1 --> P3[Phase 3\nApplication]
    P2 --> P3
    P3 --> P4[Phase 4\nPresentation]
    P2 --> P4
    P4 --> P5[Phase 5\nIntegration]
    P3 --> P5
    P5 --> P6[Phase 6\nRelease]
```

---

## 12. Closing Note

This is the sixteenth and final document of the set. `00_MASTER_PROMPT.md` opened by stating that an autonomous agent's first responsibility is to read that document before touching any code. This document is where that instruction cashes out into an actual first action: start at T-001.

---

## 13. Revision History

| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-07-12 | Initial seed backlog, derived from `00`–`14`, all v1.0.0. Phases 0–2 fully broken down; phases 3–6 intentionally coarse, per §1. |
| 1.0.1 | 2026-07-22 | Phase 0 (T-001–T-006) marked Done; see ADR 0002. Added NC-004 (Checkstyle vs. `///` Markdown JavaDoc). |
| 1.0.2 | 2026-07-22 | Phase 1 T-101–T-103 (domain core) marked Done; see ADR 0003. NC-004 resolved (doclint). Coverage gate refined to per-module floor (ADR 0003 §8). |
| 1.0.3 | 2026-07-22 | Phase 1 T-104–T-107 (three estimators + Butterworth filter + FFT + spectral confidence) marked Done; see ADR 0004. NC-002 rehomed to T-302. |
| 1.0.4 | 2026-07-23 | Phase 1 T-108 (`SignalQualityStateMachine`) marked Done. Only T-109 (golden-file fixtures, needs recorded data) remains open in Phase 1. |
| 1.0.5 | 2026-07-23 | Phase 2 T-204/T-205 (SQLite schema, migration runner, `SqliteMeasurementRepository`) marked Done; see ADR 0005. `MeasurementSession`/`SessionSummary` reconciled to the binding schema. |
| 1.0.6 | 2026-07-23 | Phase 3 T-301 (six discrete use cases) marked Done; Mockito-tested. Start/End live-pipeline wiring deferred to T-302. |
| 1.0.7 | 2026-07-23 | NC-001 direction resolved (MediaPipe BlazeFace + Face Mesh via ONNX; ADR 0006); T-202 In Progress (validation pending). Docs-only. |
| 1.0.8 | 2026-07-23 | T-302 part 1: `MeasurementObserver` + `MeasurementPipeline` core, `Frame` canonical RGB, `RoiSpatialAverager` (ADR 0007). Threaded orchestrator remains. |
| 1.0.9 | 2026-07-23 | T-302/T-303/T-304 Done: threaded `LiveMeasurementOrchestrator` (2 virtual-thread executors, depth-3 drop-oldest queue, cooperative cancel + drain, ScopedValue) with deterministic concurrency tests (ADR 0008). Phase 3 complete except persistenceExecutor/use-case wiring (→ T-501). |
| 1.0.10 | 2026-07-23 | Phase 4 ViewModel foundation: JavaFX 25.0.4 + XChart deps, `ConfidenceTier`, `UiThreadExecutor`, and the Live/History ViewModels, tested headless (ADR 0009). T-402/403/405 In Progress; FXML views/controllers/launcher remain (manual-checklist verified). Also fixed: rulesets moved build/→config/ (were gitignored). |

---

*End of 15_TASK.md — and of the 00–15 document set. This document is living; all others are binding governance it operates under.*