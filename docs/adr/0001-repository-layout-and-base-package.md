# ADR 0001 — Repository Layout and Base Package

- **Status:** Accepted
- **Date:** 2026-07-12
- **Decision process:** `00_MASTER_PROMPT.md §8`, node G (reversible, low-risk; simplest option chosen and recorded)

## Context

The governance document set (00–15) was authored under `src/test/docs/`, while the documents
themselves reference root-relative locations (`docs/adr/`, `docs/qa/ui-checklist.md`) and the
repository's `CLAUDE.md` instructs agents to read `docs/00_MASTER_PROMPT.md`. The pre-existing
IntelliJ scaffold used groupId `tech` with a single-module layout, while `04_PACKAGE_STRUCTURE.md §2`
designates the base package `id.asr.rppgvitals` as a replaceable placeholder.

## Decision

1. The governance document set lives at the repository root under `docs/`. `CLAUDE.md` lives at the
   repository root and points to `docs/00_MASTER_PROMPT.md`.
2. The base package **`id.asr.rppgvitals`** designated in `04_PACKAGE_STRUCTURE.md §2` is retained
   as-is (not substituted). Maven coordinates follow it: groupId `id.asr.rppgvitals`, parent
   artifactId `rppg-parent`, module artifactIds per `03_ARCHITECTURE.md §8`.
3. The IntelliJ scaffold (`tech.Main`, single-module `pom.xml`) is removed and replaced by the
   seven-module structure in `03_ARCHITECTURE.md §8` (task T-001).
4. Local build toolchain: builds run with `JAVA_HOME` set to a JDK 25 installation
   (`<maven.compiler.release>25</maven.compiler.release>` per `00 §26`); Maven 3.9.x is used.

## Consequences

- Every root-relative documentation reference in the 00–15 set now resolves without translation.
- No edit to any governance document was needed for the base-package decision; the placeholder was
  adopted verbatim.
- A future rename of the base package remains mechanical (per `04 §2`) but would touch every module;
  it should be done before Phase 1 code lands, or not at all.
