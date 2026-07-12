# 04_PACKAGE_STRUCTURE.md
# Package Structure
## rPPG Desktop Vitals Monitor

---

**Document Control**

| Field | Value |
|---|---|
| Document ID | PKG-04 |
| Version | 1.0.0 |
| Status | **BINDING** — Physical Layout Layer |
| Depends On | `03_ARCHITECTURE.md` (§4, §5, §6, §8) |
| Consumed By | `05_CODING_STANDARD.md`, `06_UI_GUIDELINE.md` |
| Precedence | Subordinate to `03_ARCHITECTURE.md`. This document does not introduce new components — every package below exists to hold a component already named in `03`. If a needed package has no obvious owner here, `03_ARCHITECTURE.md` is incomplete and must be amended first, per `00 §8`. |
| Maintainer | Human Project Architect — Abdi Soleh Rosadi |
| Last Updated | 2026-07-12 |

---

## 1. Purpose of This Document

`03_ARCHITECTURE.md` names components — `LiveMeasurementOrchestrator`, `OpenCvFrameSource`, `SqliteMeasurementRepository` — and assigns them to one of seven Maven modules. It does not say which Java package each one lives in, what a new file's path should be, or how the Dependency Rule (`00 §9`) is mechanically enforced rather than merely stated. This document answers exactly those questions, and only those questions — it introduces no new architectural concepts.

Its two consumers use it differently:

- `05_CODING_STANDARD.md` assumes this layout when specifying import ordering, package-declaration conventions, and file-header expectations.
- `06_UI_GUIDELINE.md` assumes the `presentation.javafx.*` layout below when specifying where new FXML views and their resources go.

Any agent creating a new file consults this document first to determine its path. A file placed outside the structure below is a defect, not a stylistic choice, per `00 §20.1`.

---

## 2. Base Package and Naming Convention

**Base package:** `id.asr.rppgvitals`

This is a placeholder following the reverse-domain Maven convention, chosen from the project owner's identifier in the absence of a registered domain. It should be replaced at project setup time if a different domain or GitHub-username-based identifier (`io.github.<username>.rppgvitals`) is preferred — every path in this document simply appends onto this root, so the substitution is mechanical.

Rules:

- All packages are lowercase, no underscores, no camelCase — standard Java package convention.
- The second segment after the base package is always one of the four architectural layers: `domain`, `application`, `infrastructure`, `presentation` — with one exception, `bootstrap` (§4), which holds the composition root and does not belong to any single layer by definition.
- Below the layer segment, packages are organized **by feature, never by type** — this restates `00 §20.3` concretely: there is no `domain.model`, `domain.interfaces`, or `domain.services` catch-all. There is `domain.signal`, `domain.estimation`, and so on, each containing whatever mix of records, sealed types, and interfaces that feature needs.

---

## 3. Module-to-Package Mapping

| Maven Module (`03 §8`) | Package Root | Feature Packages Inside |
|---|---|---|
| `rppg-domain` | `id.asr.rppgvitals.domain` | `capture`, `detection`, `signal`, `estimation`, `session`, `exception` |
| `rppg-application` | `id.asr.rppgvitals.application` | `usecase.measurement`, `usecase.history`, `usecase.device` |
| `rppg-infrastructure-capture` | `id.asr.rppgvitals.infrastructure.capture.opencv` | (single-purpose module, one feature package) |
| `rppg-infrastructure-inference` | `id.asr.rppgvitals.infrastructure.inference.onnx` | (single-purpose module, one feature package) |
| `rppg-infrastructure-persistence` | `id.asr.rppgvitals.infrastructure.persistence.sqlite` | (single-purpose module, one feature package) |
| `rppg-presentation` | `id.asr.rppgvitals.presentation.javafx` | `dashboard`, `history` |
| `rppg-app` | `id.asr.rppgvitals.bootstrap` | (composition root, one package) |

The three infrastructure modules are deliberately single-package. Each one exists to implement exactly one port (`03 §4`); a second package inside `rppg-infrastructure-capture`, for instance, would be a sign that module is taking on a second responsibility and should be reconsidered against `03 §6.3`, not merely reorganized.

---

## 4. Complete Package Tree

```
id.asr.rppgvitals
├── domain                                          [rppg-domain]
│   ├── capture
│   │   ├── Frame                       (record)
│   │   ├── CaptureConfiguration        (record)
│   │   ├── FrameSource                 (port)
│   │   └── package-info
│   ├── detection
│   │   ├── RegionOfInterest            (record)
│   │   ├── InferenceEngine             (port)
│   │   └── package-info
│   ├── signal
│   │   ├── PpgSample                   (record)
│   │   ├── PpgWaveform                 (record)
│   │   ├── SignalQuality               (sealed)
│   │   └── package-info
│   ├── estimation
│   │   ├── HeartRateEstimate           (record)
│   │   ├── SignalEstimator             (port)
│   │   ├── PosSignalEstimator
│   │   ├── ChromSignalEstimator
│   │   ├── GreenChannelSignalEstimator
│   │   └── package-info
│   ├── session
│   │   ├── MeasurementSession          (entity)
│   │   ├── SessionSummary              (record)
│   │   ├── MeasurementRepository       (port)
│   │   └── package-info
│   └── exception
│       ├── RppgApplicationException    (sealed root)
│       ├── CameraUnavailableException
│       ├── SignalQualityException
│       ├── ModelInferenceException
│       ├── PersistenceException
│       ├── ConfigurationException
│       └── package-info
├── application                                     [rppg-application]
│   └── usecase
│       ├── measurement
│       │   ├── StartMeasurementSessionUseCase
│       │   ├── EndMeasurementSessionUseCase
│       │   ├── LiveMeasurementOrchestrator
│       │   ├── MeasurementObserver
│       │   └── package-info
│       ├── history
│       │   ├── ListSessionHistoryUseCase
│       │   ├── GetSessionDetailUseCase
│       │   ├── DeleteSessionUseCase
│       │   └── package-info
│       └── device
│           ├── ListAvailableCameraDevicesUseCase
│           └── package-info
├── infrastructure
│   ├── capture.opencv                              [rppg-infrastructure-capture]
│   │   ├── OpenCvFrameSource
│   │   └── package-info
│   ├── inference.onnx                              [rppg-infrastructure-inference]
│   │   ├── OnnxInferenceEngine
│   │   └── package-info
│   └── persistence.sqlite                          [rppg-infrastructure-persistence]
│       ├── SqliteMeasurementRepository
│       └── package-info
├── presentation.javafx                             [rppg-presentation]
│   ├── dashboard
│   │   ├── LiveMeasurementController
│   │   ├── LiveMeasurementViewModel
│   │   └── package-info
│   └── history
│       ├── SessionHistoryController
│       ├── SessionHistoryViewModel
│       └── package-info
└── bootstrap                                       [rppg-app]
    ├── Main
    ├── CompositionRoot
    └── package-info
```

Every type named here corresponds to a component introduced in `03_ARCHITECTURE.md §3`, §4, §5, or §6. No type appears here that wasn't already named there, and no type named there is missing here.

---

## 5. Package-Level Rules

1. **Every package listed above has a `package-info.java`**, per `00 §20.1`, stating the package's single responsibility in one paragraph. This is not optional documentation — it is the first thing an agent (or a reviewer) reads before touching a package it hasn't worked in before.
2. **No package grows a second responsibility silently.** If `domain.session` starts accumulating types that aren't about a `MeasurementSession`'s identity or lifecycle, that is a signal to introduce a new feature package, not to keep adding to this one — restating `00 §20.3`'s size discipline in concrete terms for this specific tree.
3. **A new feature package requires a `03_ARCHITECTURE.md` update first.** This document only assigns packages to components that architecture has already approved; it does not independently decide that a new feature deserves a new package.
4. **Cross-package imports within `rppg-domain` are allowed** (e.g., `domain.estimation` naturally depends on `domain.signal`'s `PpgWaveform`) — the Dependency Rule (`00 §9`) constrains which *layers* may depend on which, not whether features within the Domain layer may reference one another.

---

## 6. Resource File Placement

Non-Java resources live under `src/main/resources`, mirroring the package path of the class they belong to, per standard Maven convention:

| Resource | Location |
|---|---|
| FXML views | `rppg-presentation/src/main/resources/id/asr/rppgvitals/presentation/javafx/dashboard/live-measurement.fxml` (colocated with `LiveMeasurementController`'s package path) |
| CSS stylesheets | `rppg-presentation/src/main/resources/css/*.css` (shared across views, not per-package) |
| Logging configuration | `rppg-app/src/main/resources/logback.xml` — the composition root owns runtime configuration, per `00 §18` |
| SQLite schema DDL | `rppg-infrastructure-persistence/src/main/resources/schema/*.sql` — plain DDL scripts executed by `SqliteMeasurementRepository` at startup; the exact mechanism is `10_DATABASE.md`'s responsibility |

---

## 7. Test Source Mirroring

`src/test/java` mirrors `src/main/java` package-for-package, in the same Maven module as the code under test — a test for `domain.estimation.PosSignalEstimator` lives at `rppg-domain/src/test/java/id/asr/rppgvitals/domain/estimation/PosSignalEstimatorTest.java`. Integration tests that exercise an adapter against a real (but local/temporary) resource — e.g., `SqliteMeasurementRepository` against a temp-file database — live alongside unit tests in the same module, distinguished by naming (`*IntegrationTest`) and by the CI profile defined in `13_TESTING.md`, not by a separate package tree.

---

## 8. Dependency Rule Enforcement Mechanism

`00 §21.2` requires a concrete enforcement mechanism for the Dependency Rule, "module system or architectural-fitness-function test." This project uses both, at two different grains:

**Primary — Maven module boundaries.** This is already the majority of the enforcement, and it costs nothing extra: `rppg-domain`'s `pom.xml` never declares JavaFX, OpenCV, ONNX Runtime, or the SQLite JDBC driver as a dependency. A class in `domain.*` attempting to import one of those types is a compile-time failure by construction — there is no code-review step required to catch it. The module graph in `03 §8` **is** the enforcement for all inter-module rules, including infrastructure-to-infrastructure non-coupling (`infrastructure-capture` and `infrastructure-persistence` simply never depend on each other).

**Secondary — ArchUnit test suite**, living in `rppg-app`'s test sources (the only module with the full assembled dependency graph on its test classpath). This catches what module boundaries alone cannot:

| Rule | Verifies |
|---|---|
| Domain Isolation | No class in `domain..` depends on `javafx..`, `org.opencv..`, `ai.onnxruntime..`, or `org.sqlite..` — a redundant, defense-in-depth check against a future accidental `pom.xml` change. |
| Application Purity | No class in `application..` depends on `javafx..`. |
| Sealed Exception Closure | Every class extending `RppgApplicationException` resides in `domain.exception` — nowhere else. |
| Package Naming | No package outside the tree in §4 exists under the base package without a corresponding entry in this document. |
| No God Packages | No feature package exceeds the size guidance in `00 §20.3` without a recorded justification. |

**Considered and deferred — JPMS (`module-info.java`).** Full Java Platform Module System modularity would give even stronger, compiler-enforced isolation than Maven module boundaries alone. It was deliberately not adopted for V1: OpenCV's and ONNX Runtime's Java bindings are not fully modularized, and forcing strict JPMS `requires`/`exports` declarations against automatic modules and split packages introduces real friction (`00 §16`'s native-library caution applies here directly) disproportionate to the marginal benefit over the Maven-plus-ArchUnit combination above. This may be revisited if the native-library ecosystem's JPMS support matures — any such change follows `00 §8`'s decision process, not a silent migration.

---

## 9. Relationship to Other Documents

| Document | What It Inherits From This Document |
|---|---|
| `05_CODING_STANDARD.md` | Import ordering, package-declaration, and file-header conventions are specified against the exact tree in §4. |
| `06_UI_GUIDELINE.md` | New views and resources are placed per §6's FXML/CSS convention. |
| `13_TESTING.md` | The unit/integration test split in §7 is the physical layout that document's tooling configuration must match. |

---

## 10. Revision History

| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-07-12 | Initial ratified version, derived from `03_ARCHITECTURE.md` v1.0.0. |

---

*End of 04_PACKAGE_STRUCTURE.md. Subordinate to `03_ARCHITECTURE.md`; binding on all documents listed in §9.*