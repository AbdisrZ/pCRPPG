# 14_DEPLOYMENT.md
# Deployment and Packaging
## rPPG Desktop Vitals Monitor

---

**Document Control**

| Field | Value |
|---|---|
| Document ID | DEP-14 |
| Version | 1.0.0 |
| Status | **BINDING** — Release Engineering Specification |
| Depends On | `03_ARCHITECTURE.md` (§8), `00_MASTER_PROMPT.md` (§26), `13_TESTING.md` (§6) |
| Consumed By | `15_TASK.md` |
| Precedence | Subordinate to `00_MASTER_PROMPT.md §26`, which this document is required to resolve concretely — "resolved per-platform via Maven profiles/classifiers" is a promise this document keeps. |
| Maintainer | Human Project Architect — Abdi Soleh Rosadi |
| Last Updated | 2026-07-12 |

---

## 1. Purpose of This Document

`02` NFR-201 and NFR-202 require the application to run on Windows, macOS, and Linux as a self-contained installable artifact requiring no separate JDK installation. `00 §26` promises that native-library platform resolution is handled via Maven profiles and classifiers, without specifying the mechanism. This document is that mechanism: the actual packaging tool, the actual per-platform build process, and the actual release sequence — the last step before `13_TESTING.md`'s pre-release gate becomes a shipped artifact.

---

## 2. Packaging Tool and Strategy

The application is packaged using the JDK's own **`jlink`** and **`jpackage`** tools — no third-party packaging framework is introduced, avoiding a `00 §16`-style dependency decision for what the JDK already provides.

Because the application is deliberately **not** fully JPMS-modularized (`04 §8`'s reasoning: OpenCV, ONNX Runtime, and JavaFX are not consistently modularized upstream), packaging uses `jpackage`'s non-modular, classpath-based mode rather than a fully modular `jlink`-only image of the whole application:

1. `jlink` builds a minimal custom Java runtime image containing only the standard JDK modules the application actually needs (`java.base`, `java.desktop`, `java.sql`, and others as identified during the build) — this step only ever operates on the JDK's own, properly modularized modules, never on the application's own non-modular dependencies.
2. `jpackage` takes that custom runtime image and the application's full classpath — all seven modules' JARs (`03 §8`), plus JavaFX's platform-specific JARs, plus OpenCV's and ONNX Runtime's platform-specific native bindings (§3) — and produces a native, self-contained installer.

This two-step process is the standard, well-documented pattern for packaging a JavaFX desktop application that is not fully modularized, and is chosen specifically because it does not require resolving JPMS compatibility for libraries that were never designed with it in mind.

---

## 3. Native Library Bundling Per Platform

Realizing `00 §26`'s Maven-profile-and-classifier requirement:

- Separate Maven profiles exist per target platform/architecture combination (Windows x86-64, macOS x86-64, macOS ARM64, Linux x86-64), each pulling in the correctly platform-classified artifacts for OpenCV, ONNX Runtime, and JavaFX — all three publish platform-specific native binaries under Maven classifiers, and mixing classifiers across platforms is a build-time failure, not a runtime one, per `00 §26`'s "fails loudly... rather than a cryptic `UnsatisfiedLinkError`" requirement.
- `jpackage` itself does not cross-compile installers — a Windows `.msi` is produced by running the packaging step on Windows, a macOS `.dmg` on macOS, and Linux packages on Linux. Consequently, the CI pipeline (`13_TESTING.md`) runs the packaging stage on one CI runner per target platform, not on a single runner attempting to produce all three.

---

## 4. Platform-Specific Installer Artifacts

| Platform | Artifact Type | `jpackage` Type |
|---|---|---|
| Windows | Installer package | `msi` |
| macOS | Disk image or installer package | `dmg` (primary) or `pkg` |
| Linux | Distribution-native package | `deb` and `rpm`, both produced, since neither covers the full range of common Linux distributions alone |

Each artifact embeds: the custom runtime image (§2), all application and third-party JARs, native libraries for its specific platform (§3), and the application icon and metadata (name, version, publisher).

---

## 5. First-Run Bootstrap

On first launch on a given machine, the composition root (`rppg-app`, `03 §8`) is responsible for:

1. Creating the platform-appropriate application-data directory if it does not already exist (`10 §8`'s three OS-specific paths).
2. Initializing the SQLite database file and applying the initial schema (`10 §5`) within that directory.
3. Writing the log file destination (§7) into the same directory, so a user experiencing a problem and a developer diagnosing it both know exactly where to look without hunting.

This bootstrap is idempotent — a second launch on a machine where these already exist does nothing destructive, it simply proceeds.

---

## 6. Versioning Scheme

The application follows Semantic Versioning (`MAJOR.MINOR.PATCH`). The running application's version string is what populates the `app_version` column in `10 §3.1`'s `sessions` table (`02 §5` DR-3) — every persisted session is traceable to the exact application version that recorded it. This versioning is independent of the 00–15 document set's own per-document version numbers (`00 §42` and equivalents) — the two track different things and are not expected to move in lockstep.

---

## 7. Logging Configuration in Packaged Builds

- The packaged application's `logback.xml` (`04 §6`) defaults to `INFO` level, consistent with `00 §18`'s rule that `DEBUG` is disabled by default in packaged builds and `TRACE` is never enabled outside local development.
- Log output is written to a file within the application-data directory (§5), not only to a console the end user is unlikely to see — a packaged desktop application launched by double-clicking an icon has no visible console, so console-only logging would be effectively invisible in the field.
- Log file rotation (size- or time-based) is configured to prevent unbounded growth over a long-lived installation, without introducing a new dependency — Logback's own built-in rolling-file appender covers this.

---

## 8. Release Process

1. `13_TESTING.md §6`'s pre-release gate passes in full — including the manual UI checklist and a packaging dry run — before a release is cut, per `00 §19.3`.
2. The version number (§6) is bumped following Semantic Versioning based on the nature of the changes since the last release.
3. `jpackage` (§2) is run once per target platform (§3, §4), producing the three installer artifacts.
4. Each artifact is smoke-tested — installed and launched on a clean or representative machine for its platform — before being published; a release that has never actually been installed from its own installer is not considered verified.
5. Release notes reference the relevant `15_TASK.md` items completed since the previous release, giving every shipped change a traceable origin.

---

## 9. Relationship to Other Documents

| Document | What It Inherits From This Document |
|---|---|
| `15_TASK.md` | Packaging and release tasks are scoped against §8's process, not treated as an undifferentiated "ship it" step. |

---

## 10. Revision History

| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-07-12 | Initial ratified version, resolving `00_MASTER_PROMPT.md §26`'s deferred packaging mechanism. |

---

*End of 14_DEPLOYMENT.md. Subordinate to `00_MASTER_PROMPT.md §26` and `03_ARCHITECTURE.md`; binding on `15_TASK.md`.*