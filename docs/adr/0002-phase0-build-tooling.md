# ADR 0002 — Phase 0 Build Tooling and Quality-Gate Realisation

- **Status:** Accepted
- **Date:** 2026-07-22
- **Decision process:** `00_MASTER_PROMPT.md §8`, node G (reversible, low-risk) for the tool/version
  and wiring choices; node H / `§40` escalation for the Checkstyle-vs-Markdown-JavaDoc conflict
  (recorded here and as NC-004 in `15_TASK.md`).
- **Implements:** Phase 0 tasks T-002–T-006 (`15_TASK.md`).

## Context

Phase 0 requires the five-tool static-analysis chain (`05 §13`), JaCoCo coverage gates (`13 §7`),
native-library platform profiles (`14 §3`), and a reproducible build (`00 §26`), all enforced by
`mvn verify`. The governance fixes the *tools* and *thresholds* but leaves the *versions*, *ruleset
contents*, and *wiring mechanisms* to the implementer. The build also had no reproducible entry
point: no `mvn` on `PATH` and no committed wrapper, despite `.gitignore` already whitelisting a
wrapper jar. All decisions below were validated against a green `mvn verify` and `mvn -P ci verify`
on JDK 25 (`C:\Program Files\Java\jdk-25`).

## Decisions

### 1. Maven Wrapper (reproducibility, `00 §26`)
A jar-less Maven Wrapper (`distributionType=only-script`) pinned to **Maven 3.9.9** is committed
(`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`). The script form avoids committing a
binary jar. `./mvnw` is now the canonical build entry point.

### 2. Formatter compiler-internals access (`.mvn/jvm.config`)
Palantir Java Format (used by Spotless) needs `--add-exports`/`--add-opens` into `jdk.compiler` on
JDK 16+. These are committed in `.mvn/jvm.config` so every Maven invocation — local and CI — can run
the formatter on JDK 25.

### 3. Pinned tool versions (`00 §16`, no ranges)
Chosen as the current releases that parse/analyse Java 25 sources and bytecode (major version 69):
Spotless **3.8.0** + Palantir Java Format **2.96.0**; maven-checkstyle-plugin **3.6.0** + Checkstyle
**10.26.1**; maven-pmd-plugin **3.28.0** + PMD **7.26.0**; spotbugs-maven-plugin **4.10.3.0**;
jacoco-maven-plugin **0.8.15**; ArchUnit **1.4.2**. Earlier candidate versions failed on JDK 25
(Palantir 2.50.0 `NoSuchMethodError` on javac internals; PMD plugin 3.26.0 rejected `targetJdk 25`;
ArchUnit 1.3.0 `Unsupported class file major version 69`) and were rejected.

### 4. Ruleset scope — no formatter/linter overlap
Spotless owns all formatting including import grouping (`05 §2/§3`). Checkstyle enforces only the
*structural* rules Spotless does not: no-wildcard/unused imports, member declaration order (`05 §5`),
tab prohibition, and package-info presence (`04 §5`). `JavadocPackage` is scoped to main sources via
`checkstyle-suppressions.xml` (test packages carry no package-info, per `04 §7`). PMD carries the
size/complexity metrics of `00 §10` (report levels set one above each threshold so a value exactly
at the limit passes). PMD `targetJdk` was dropped in favour of auto-detection to avoid the plugin's
version-allowlist.

### 5. JavaDoc-presence enforcement deferred from Checkstyle to javac doclint (node H)
`05 §8` mandates JEP 467 Markdown documentation comments (`///`). Every Checkstyle version tested
through **13.8.0** fails to recognise `///` as JavaDoc for its `MissingJavadocType`/
`MissingJavadocMethod` checks — they false-flag correctly documented members. Enforcing them would
trap every Phase-1 `///`-documented type. `05 §13` assigns "JavaDoc presence" to Checkstyle, so this
is a genuine doc-vs-tooling conflict the document set did not anticipate. **Resolution:** the two
MissingJavadoc checks are removed from the Checkstyle config now; JavaDoc presence will be enforced
by javac **`-Xdoclint`**, which natively understands `///` (verified: documented members pass, missing
members error under `-Werror`). Wiring doclint is deferred to **T-101**, the first task to introduce
public production types, so its configuration (notably doclint's implicit-default-constructor
warning) can be tuned against real code rather than guessed at now. Tracked as **NC-004**.

### 6. Coverage gate wiring (`13 §7`)
Per-module JaCoCo `check` (≥ 90% line / 85% branch) lives in `rppg-domain`; the project-wide
aggregate (≥ 80% line) is realised in `rppg-app` (the reactor's last module) by `merge`-ing every
module's `jacoco.exec` then `check`-ing the merged data. With no production classes yet both bundles
are empty and the checks pass vacuously; they begin to bite the moment Phase 1 code lands.

### 7. Native-platform profiles (`14 §3`)
Four OS/arch-activated profiles (`windows-x86_64`, `macos-x86_64`, `macos-aarch64`, `linux-x86_64`)
set a `native.classifier` property for the Phase 2/4 native-bearing dependencies to consume; no
native dependency is declared yet. An unsupported platform is caught at build time by an enforcer
`requireProperty` rule (a build-time plugin sees the merged property set, unlike profile activation,
which cannot — an earlier `!native.classifier`-activated fail profile was rejected for that reason).

### 8. package-info scope (resolves the `04 §5.1` ambiguity)
`package-info.java` is created for the **15 leaf packages** marked in the `04 §4` tree. Intermediate
structural packages (`domain`, `application.usecase`, `presentation.javafx`, …) contain no types and
receive none.

## Consequences

- `./mvnw clean verify` and `./mvnw -P ci clean verify` both pass on JDK 25 with all gates active.
- The gates are proven live: a mis-formatted file fails `spotless:check`; a public method without a
  doc comment fails the intended JavaDoc gate mechanism.
- One governance gap (`05 §13`'s Checkstyle-owned JavaDoc presence) is reassigned to doclint, pending
  human confirmation via NC-004. No other governance document required amendment.
- Tool versions will need periodic revision as the JDK advances; any bump follows `00 §8`.
