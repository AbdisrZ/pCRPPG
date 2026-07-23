# rPPG Desktop Vitals Monitor

A desktop-native application that estimates physiological signals — primarily heart rate, and
where signal quality allows, heart-rate variability (HRV) — from an unmodified RGB webcam feed
using remote photoplethysmography (rPPG). Built to production-grade engineering standards on
**Java 25 (LTS)** with a clean/hexagonal architecture.

> **Not a medical device.** This project is explicitly not a certified or clinically validated
> medical device and makes no diagnostic claims. See `docs/00_MASTER_PROMPT.md` §4.

## Governance

All work on this repository is governed by the binding document set in [`docs/`](docs/),
`00_MASTER_PROMPT.md` through `15_TASK.md`. **Read `docs/00_MASTER_PROMPT.md` first.** The living
backlog is [`docs/15_TASK.md`](docs/15_TASK.md); architecture decisions are recorded under
[`docs/adr/`](docs/adr/).

## Prerequisites

- **JDK 25** (LTS). The build targets `--release 25` and the Maven Enforcer requires a `[25,)` JDK.
- No system Maven install is needed — the repository ships a **Maven Wrapper** (`./mvnw`) pinned to
  Maven 3.9.9.

Point `JAVA_HOME` at a JDK 25 installation before building, for example:

```bash
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"

# macOS / Linux
export JAVA_HOME=/path/to/jdk-25
```

## Build

A single command runs compilation, the full static-analysis chain, all tests, and the coverage
gates (`00_MASTER_PROMPT.md` §36):

```bash
./mvnw clean verify           # macOS / Linux
mvnw.cmd clean verify         # Windows
```

For the strict CI profile (adds `-Xlint:all -Werror`):

```bash
./mvnw -P ci clean verify
```

Every commit must pass `mvnw verify` from a clean checkout.

## Module layout

Seven Maven modules realise the four architectural layers (`docs/03_ARCHITECTURE.md` §8):

| Module | Layer | Responsibility |
|---|---|---|
| `rppg-domain` | Domain | Framework-free value objects, ports, and signal estimators |
| `rppg-application` | Application | Use cases and the live-measurement orchestrator |
| `rppg-infrastructure-capture` | Infrastructure | OpenCV frame-capture adapter |
| `rppg-infrastructure-inference` | Infrastructure | ONNX Runtime inference adapter |
| `rppg-infrastructure-persistence` | Infrastructure | SQLite persistence adapter |
| `rppg-presentation` | Presentation | JavaFX views, controllers, and view models |
| `rppg-app` | Composition root | Wiring, entry point, and the ArchUnit fitness suite |

## Quality gates

Run inside `mvn verify` and fail the build on any violation (`docs/05_CODING_STANDARD.md` §13):

- **Spotless** (Palantir Java Format) — formatting and import order
- **Checkstyle** — structural rules (imports, member order, package-info presence)
- **PMD** — cyclomatic complexity, method/class length
- **SpotBugs** — correctness and null-safety
- **ArchUnit** — Dependency-Rule and package-tree conformance (`docs/04_PACKAGE_STRUCTURE.md` §8)
- **JaCoCo** — coverage: domain ≥ 90% line / 85% branch, project ≥ 80% line
