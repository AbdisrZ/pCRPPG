# 13_TESTING.md
# Testing Strategy
## rPPG Desktop Vitals Monitor

---

**Document Control**

| Field | Value |
|---|---|
| Document ID | TST-13 |
| Version | 1.0.0 |
| Status | **BINDING** — Verification Layer |
| Depends On | `00_MASTER_PROMPT.md` (§14, §34, §36), `04_PACKAGE_STRUCTURE.md` (§7, §8) |
| Consumed By | `14_DEPLOYMENT.md` |
| Precedence | Subordinate to `00_MASTER_PROMPT.md §14/§34`. This document assembles the concrete CI pipeline those sections require; it does not loosen any gate they establish. |
| Maintainer | Human Project Architect — Abdi Soleh Rosadi |
| Last Updated | 2026-07-12 |

---

## 1. Purpose of This Document

Nearly every document in this set has, in passing, promised some form of verification: `02 §8` promised a verification method per requirement class, `04 §8` promised ArchUnit rules, `06 §9`/`06 §11` promised a manual UI checklist, `12 §8`/`12 §9` promised a benchmark methodology and regression gate, `05 §13` promised a five-tool static analysis chain. None of those documents assembled these into one CI pipeline with a defined order and defined gates. This document is that assembly — the single place an agent or contributor looks to answer "what actually has to pass before this can merge, and what only has to pass before a release."

---

## 2. Test Suite Inventory

| Suite | Tooling | Scope | Lives In |
|---|---|---|---|
| Unit tests | JUnit 5, Mockito | Domain and application logic in isolation (`00 §14`, ~70% of the suite) | Each module's `src/test/java`, mirroring `src/main/java` (`04 §7`) |
| Integration tests (`*IntegrationTest`) | JUnit 5 | Adapters against real-but-local resources: `SqliteMeasurementRepository` against a temp-file database, `OpenCvFrameSource` against recorded video fixtures | Same module as the adapter under test, distinguished by naming (`04 §7`) |
| Golden-file signal tests | JUnit 5 | `07`'s three `SignalEstimator` implementations, end-to-end against fixtures with known reference heart rate (§3) | `rppg-domain/src/test/resources/fixtures/` |
| Architectural fitness tests | ArchUnit | Layer and dependency-direction rules (`04 §8`) | `rppg-app`'s test sources — the only module with the full assembled dependency graph |
| Microbenchmarks | JMH | Hot-path component cost: `SignalEstimator` implementations, the FFT estimation step, a single `InferenceEngine` call (`12 §8`) | A dedicated `benchmarks` source set, separate from the regular test source set, so `mvn test` does not silently run multi-minute JMH suites |
| Instrumented latency test | JUnit 5 + manual timestamping | End-to-end camera-to-display latency across the real thread topology (`12 §8`, `11 §3`) | `rppg-app` integration tests |
| Fault-injection tests | JUnit 5 + Mockito | Simulated camera disconnection, corrupted/empty frame streams, forced termination mid-write (`02 §8` Reliability) | `rppg-infrastructure-*` and `rppg-app` integration tests |
| Security/privacy assertions | JUnit 5 | Zero outbound network sockets during a session lifecycle; no image data present in the SQLite file post-session (`02 §8`) | `rppg-app` integration tests |
| Manual UI checklist | Documented checklist, human-executed | `06`'s screen specifications and accessibility requirements (§4) | `docs/qa/ui-checklist.md` |

---

## 3. Golden-File Fixture Strategy

Realizing `00 §14`'s golden-file requirement concretely:

- Each fixture is a short recorded video clip (30–60 seconds) paired with a **ground-truth heart rate reference** — either sourced from a public rPPG benchmark corpus with accompanying reference signals (e.g., UBFC-rPPG, a commonly used dataset for exactly this kind of validation), or self-recorded with a simultaneous contact pulse oximeter reading where dataset licensing is a concern for a specific use.
- Fixtures are organized into categories matching the operating conditions `02 §3.4` and `08 §4` are designed to handle: `clean/` (good lighting, minimal motion), `low-light/`, `motion/`, `partial-occlusion/`. A `SignalEstimator` is only considered validated against the full category set, not just the easy `clean/` cases — a change that improves accuracy on `clean/` fixtures while regressing `motion/` fixtures is a regression, not an improvement.
- Each golden-file test asserts the estimated heart rate falls within a defined tolerance of the ground truth (e.g., ± 5 bpm for `clean/` fixtures; wider, explicitly documented tolerances for the harder categories, since perfect accuracy under motion is not a realistic bar).
- Fixture video files are managed via Git LFS rather than committed directly, keeping the main repository history free of large binary churn — this is a development-tooling choice, not a runtime dependency, and does not require the `00 §16` dependency-addition process.

---

## 4. Manual UI Checklist Execution

- The checklist is a single document (`docs/qa/ui-checklist.md`), structured as one row per `06`-specified screen and state (`06 §6.2`'s state-to-treatment table, `06 §9`'s accessibility requirements), each with a pass/fail column and a notes column.
- Per `02 §8`, it is executed by someone who has not recently seen the current build's UI — a developer checking their own just-written screen against their own mental model of what it should do is not an adequate substitute for this check.
- It runs before every release (§6), not on every commit — it is not automatable given the current toolchain, and requiring it per-commit would make the inner development loop impractically slow.
- A failed row is filed as a `15_TASK.md` item and blocks release sign-off (`00 §19.3`) until resolved or explicitly waived with a recorded reason.

---

## 5. CI Pipeline: Every-Commit Gate

Runs on every proposed change, all stages blocking (`00 §36`):

1. Compile all seven modules (`03 §8`).
2. Static analysis and formatting (`05 §13`): Spotless, Checkstyle, PMD, SpotBugs, ArchUnit.
3. Unit tests.
4. Integration tests.
5. Golden-file signal tests (§3) — these are fast enough (fixed, small fixture set, no live camera) to run on every commit, unlike the JMH suite.
6. Fault-injection and security/privacy assertion tests.
7. Coverage check (§7).

A failure at any stage stops the pipeline — later stages do not run against code already known to be broken, per `00 §36`'s "diagnose to root cause" principle rather than collecting a pile of unrelated failures.

---

## 6. CI Pipeline: Pre-Release Gate

Runs before a release is cut, in addition to everything in §5:

1. Full JMH microbenchmark suite (`12 §8`), compared against the recorded baseline (`12 §9`) — not run per-commit because a full JMH suite takes materially longer than the rest of the pipeline combined, and hot-path performance does not meaningfully shift commit-to-commit for most changes.
2. Cold-start measurement (`12 §6`) on reference hardware (`12 §2`).
3. Manual UI checklist (§4).
4. A full packaging dry run through `14_DEPLOYMENT.md`'s pipeline on at least one target OS, per `00 §19.3`.

---

## 7. Coverage Enforcement

Coverage is measured with **JaCoCo**, integrated into the Maven build. Thresholds restate `00 §10` precisely — domain-layer modules (`rppg-domain`) at ≥ 90% line / ≥ 85% branch, overall project at ≥ 80% line — and are enforced as a build-failing gate in §5's every-commit pipeline, not a dashboard number someone checks occasionally.

---

## 8. Test Fixture Management

Non-video test data (expected JSON/CSV outputs for unit-level assertions, ArchUnit rule configuration) lives alongside the tests that use it, under each module's `src/test/resources`, per the same package-mirroring convention as test source code (`04 §7`). Video fixtures (§3) are the only test data with special handling (Git LFS), because they are the only category large enough to warrant it.

---

## 9. Flakiness Policy

An intermittently-failing test is treated as a defect at the same severity as a consistently-failing one (`00 §19.3`'s Priority-0/Priority-1 language) — never auto-retried by CI to paper over it, and never quietly skipped or marked `@Disabled` without a `15_TASK.md` entry tracking its resolution. `00 §14`'s prohibition on non-deterministic tests (timing-dependent, thread-scheduling-dependent) exists specifically so this situation is rare; when it happens anyway, the response is to fix or remove the test, not to accommodate it.

---

## 10. Relationship to Other Documents

| Document | What It Inherits From This Document |
|---|---|
| `14_DEPLOYMENT.md` | The pre-release gate in §6, including the packaging dry run, is a precondition that document's release process assumes has already passed. |

---

## 11. Revision History

| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-07-12 | Initial ratified version, assembling verification commitments from `00`, `02`, `04`, `05`, `06`, and `12`, all v1.0.0. |

---

*End of 13_TESTING.md. Subordinate to `00_MASTER_PROMPT.md §14/§34`; binding on `14_DEPLOYMENT.md`.*