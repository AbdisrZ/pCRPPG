# 05_CODING_STANDARD.md
# Coding Standard
## rPPG Desktop Vitals Monitor

---

**Document Control**

| Field | Value |
|---|---|
| Document ID | STD-05 |
| Version | 1.0.0 |
| Status | **BINDING** — Day-to-Day Writing Layer |
| Depends On | `00_MASTER_PROMPT.md` (§7, §23, §27, §33), `04_PACKAGE_STRUCTURE.md` (§4) |
| Consumed By | `14_DEPLOYMENT.md`, `15_TASK.md` |
| Precedence | Subordinate to `00_MASTER_PROMPT.md`. This document is purely mechanical — it decides *how* something already required by `00` is written, never *whether* it is required. |
| Maintainer | Human Project Architect — Abdi Soleh Rosadi |
| Last Updated | 2026-07-12 |

> **A note on this document's form.** A coding standard conventionally shows code snippets. This one deliberately does not — `00_MASTER_PROMPT.md`'s scope is documentation only, and a JavaDoc example or a logger-declaration snippet is source code by any reasonable definition, even at one line. Every rule below is stated in prose or table form, precisely enough to be unambiguous without needing an example to disambiguate it.

---

## 1. Purpose of This Document

`00_MASTER_PROMPT.md §7` and `§23` state coding philosophy and naming principles at a level meant to last five years without revision. This document is the opposite: the mechanical, frequently-referenced ruleset an agent or contributor checks *while typing* — indentation, import order, member order, JavaDoc format, logging format. If `00`'s rules are the constitution, this is the style manual every author actually keeps open.

`14_DEPLOYMENT.md` inherits the tooling chain defined in §13 (the CI build must run these checks). `15_TASK.md` inherits the commit convention in §14 for how completed tasks are recorded in version control history.

---

## 2. Formatting and Layout

| Aspect | Rule |
|---|---|
| Indentation | 4 spaces; tabs are never used. |
| Line length | 120 characters, enforced by the formatter, not manually. |
| Brace style | Opening brace on the same line as the declaration (Java/K&R style), for classes, methods, and control structures alike. |
| Blank lines | One blank line between members of a class; no more than one consecutive blank line anywhere. |
| Formatter | **Palantir Java Format**, applied via the **Spotless Maven Plugin**. Chosen over the more common google-java-format specifically for its 4-space indentation, which matches broader Java community convention and lowers onboarding friction (`00 §12`) more than the 2-space alternative would. |
| Enforcement | `mvn spotless:check` runs as part of `mvn verify` (`00 §36`) and fails the build on any formatting deviation; `mvn spotless:apply` is the only sanctioned way to fix a violation — hand-formatting to satisfy the checker is not an efficient use of anyone's time. |

---

## 3. Import Rules

Imports are grouped into three blocks, separated by one blank line, each block alphabetized:

1. Java standard library (`java.*`, `javax.*`)
2. Third-party libraries, alphabetized by fully-qualified package (`ai.onnxruntime`, `javafx.*`, `org.apache.commons.math3.*`, `org.opencv.*`, `org.sqlite.*`, and so on)
3. This project's own packages (`id.asr.rppgvitals.*`)

Rules:

- Wildcard imports (`import java.util.*`) are never used, regardless of how many members are imported from a package — this is a direct application of `00 §5`'s "explicit over implicit" principle to import statements specifically.
- Static imports are permitted only in test code, and only for well-known, universally recognized static members (JUnit 5's `Assertions` methods, Mockito's `when`/`verify`/`mock`). Production code does not use static imports.
- An import of a type from a layer the current class is not permitted to depend on (`04 §8`'s ArchUnit rules) should never compile in the first place; if it does, that is a build configuration defect to escalate, not a style violation to quietly work around.

---

## 4. Naming Conventions

Elaborating `00 §23` with concrete per-kind guidance:

| Kind | Convention | Example Shape |
|---|---|---|
| Class / Record / Interface | `PascalCase`, noun or noun phrase | `HeartRateEstimate`, `FrameSource` |
| Port (Domain-owned interface) | Named for the role it plays, no `I`-prefix, no `Port` suffix | `FrameSource`, not `IFrameSource` or `FrameSourcePort` |
| Adapter (Infrastructure implementation) | `{Technology}{PortName}` | `OpenCvFrameSource` implements `FrameSource` |
| Use case | `{Verb}{Noun}UseCase` | `StartMeasurementSessionUseCase` |
| Sealed type permitted subtype | Descriptive of the specific state/variant, not a generic suffix like `Type1` | `SignalQuality.Stable`, `SignalQuality.Degraded` |
| Method | `camelCase`, verb-first | `estimate`, `nextFrame`, `isDeviceAvailable` |
| Boolean-returning method | Reads as a predicate | `isSignalStable()`, `hasValidFace()` — never `checkSignal()` or `signalStatus()` |
| Constant | `UPPER_SNAKE_CASE`, `static final` | `MAX_QUEUE_DEPTH` |
| Test method | `unitUnderTest_condition_expectedOutcome` | `estimateHeartRate_withLowSignalQuality_returnsUnreliableResult` |
| Type parameter (generics) | Single uppercase letter, or a short descriptive name for non-trivial generic APIs | `T`, or `SampleType` where a bare letter would be unclear |

The domain glossary (`00 §23`, detailed in `07_SIGNAL_PROCESSING.md` and `08_ESTIMATOR_ENGINE.md`) takes precedence over generic naming instinct — "ROI," "rPPG," "HR," "HRV," "SNR," and "fps" are always spelled exactly this way, never expanded or abbreviated differently for variety.

---

## 5. Class and Record Member Ordering

For an explicit class (not a record):

1. Static fields — `public static final` constants first, then `private static final`.
2. Instance fields, `final` fields before non-`final`.
3. Constructors.
4. Static factory methods, if any (immediately after constructors).
5. Public instance methods, grouped by logical purpose rather than alphabetized.
6. Package-private and `protected` methods.
7. Private instance methods, ordered by the step-down rule — a private method appears after the public method that calls it, not alphabetically or by size.

For a record, the compact constructor (if present) is the only thing that precedes the record's own generated accessors conceptually; any additional derived methods (computed properties not part of the canonical state) follow after. Records do not manually declare `equals`, `hashCode`, or `toString` overrides of the generated versions without a specific, commented reason — overriding a record's generated identity methods without cause defeats the reason to use a record at all.

---

## 6. Java 25 Style Guidance

Elaborating the feature policy in `00 §27` into concrete when-to-use guidance:

- **`var`** is used only where the right-hand side makes the type unambiguous to a reader at a glance — a constructor call or a well-named factory method. It is never used for primitive literals, never for a method's declared return type, and never where the inferred type requires the reader to look elsewhere to know what it is.
- **Record vs. explicit class**: default to a record for any type that is a pure data carrier with structural equality — this covers the large majority of domain types in `03 §3`. Use an explicit class only when the type has identity-based equality over time (`MeasurementSession`, correctly modeled as an Entity, not a Value Object) or requires internal state that a compact constructor cannot adequately validate or encapsulate.
- **Records with array or collection fields perform defensive copying inside the compact constructor.** This is not automatic — a record does not deep-copy a passed-in array or mutable collection, so without an explicit defensive copy, code outside the record could mutate what is supposed to be an immutable value after construction. Any record in this codebase holding an array or a mutable collection type is non-conformant without this.
- **Sealed types always write an explicit `permits` clause**, even in cases where the compiler could infer it from same-file or same-package placement — the clause serves as documentation at the declaration site, consistent with `00 §5`'s explicit-over-implicit principle.
- **Pattern-matching `switch` over a sealed type never includes a `default` branch.** A `default` branch defeats the entire benefit of exhaustiveness checking — if a new permitted subtype is added later, the compiler should fail every switch that doesn't yet handle it, not silently fall through to `default`.
- **Scoped Values** (`00 §15`, `00 §27`) are declared once, at a clearly identifiable location within `application.usecase.measurement` (alongside `LiveMeasurementOrchestrator`), never redeclared ad hoc in unrelated classes.

---

## 7. Null-Safety and Immutability Rules

- A method parameter that is logically required is never silently tolerant of `null` — it fails fast, per `00 §22.1`, rather than deferring the failure to wherever the `null` eventually causes a `NullPointerException`.
- A method whose result may legitimately be absent returns `Optional<T>`, never `null` — this is how `InferenceEngine`'s "no face detected" case is represented, per `00 §22.2`'s example.
- `Optional` is a return-type convention, not a field type. A record or class field is never typed as `Optional<T>` — this is a well-known Java anti-pattern (it complicates equality, hashing, and serialization for no real benefit over simply allowing the field itself to be absent-or-present through other means, such as a sealed alternative or a genuinely nullable but well-documented field at a system boundary).
- Value objects are immutable by construction (records, per `00 §7`); where a field logically cannot be reassigned after construction but isn't itself a record (rare, and requires justification per `00 §7`'s no-premature-abstraction principle), it is declared `final`.

---

## 8. JavaDoc Standard

Every `public` and `protected` class, interface, and method carries a JavaDoc comment, using the Markdown documentation-comment style enabled by JEP 467 (`00 §27`, `00 §33`) rather than legacy HTML-in-JavaDoc tags.

Required structure:

1. A one-sentence summary, third-person descriptive (e.g., "Estimates heart rate from a windowed PPG waveform," not "This method estimates...").
2. Additional explanatory paragraphs if the summary alone doesn't convey the *why*, not just the *what*.
3. A `@param` entry for every parameter, describing its meaning and any constraints — not a restatement of its type or name.
4. A `@return` entry describing the semantics of the returned value, including what an empty `Optional` or an edge-case value means.
5. A `@throws` entry for every exception the method can actually throw, stating the condition that triggers it.
6. Markdown formatting (backtick code spans for identifiers, bullet lists for enumerated conditions) is used wherever it improves clarity — this is the specific benefit JEP 467 exists to provide, and JavaDoc that ignores it in favor of dense unformatted prose is not making full use of the adopted feature.

Package-level responsibility is documented in each package's `package-info.java`, per `04 §5`, using the same Markdown style.

---

## 9. Comments Policy

- Inline comments explain **why**, never **what** — a comment restating the next line in English is noise, not documentation, per `00 §7`.
- A comment citing a published algorithm or technique (e.g., a specific rPPG extraction method) references the source by name and points to the fuller citation in `07_SIGNAL_PROCESSING.md`, rather than re-explaining the algorithm inline.
- `TODO` comments always reference a task ID from `15_TASK.md` — an unreferenced `TODO` is treated as a `00 §10` quality-gate violation, not a harmless note.
- Commented-out code is never committed. Version control history is where old code lives, not the file itself — a reviewing agent treats a block of commented-out code as equivalent in severity to dead code (`00 §19.2`).

---

## 10. Exception Handling Style

- A `try` block is scoped as narrowly as possible around the statement(s) that can actually throw — it does not wrap unrelated code "just in case," which obscures which line is actually the risk.
- A `catch` block that only rethrows a translated exception does so immediately, with the original exception set as the cause (never discarded), so the root failure remains traceable through logs (`00 §18`) even after translation at an adapter boundary (`00 §22.1`).
- Multi-catch (`catch (TypeA | TypeB e)`) is used when two exception types genuinely receive identical handling; it is not used to paper over two exceptions that should actually be handled differently.
- A resource that implements `AutoCloseable` (a native OpenCV `Mat`, a JDBC connection) is always acquired in a try-with-resources statement, never manually closed in a `finally` block — this removes an entire class of resource-leak bugs by construction, which matters directly for `00 §19.3`'s production-readiness requirement around resource cleanup.

---

## 11. Logging Style

- Every class that logs declares exactly one logger instance, private, static, and final, obtained from SLF4J's factory for that specific class — never a shared or inherited logger across unrelated classes.
- Log messages are always parameterized using SLF4J's placeholder syntax, never built via string concatenation or `String.format` — this is not just a style preference, it avoids the cost of constructing a message string when the configured log level would discard it anyway.
- A log statement never contains raw frame pixel data or waveform sample values, at any level, per `00 §18` — this is restated here because it is exactly the kind of rule that is easy to violate accidentally while adding a convenient-seeming debug line, and the coding standard is where a reviewer actually checks for it line by line.
- Every log statement emitted during an active measurement session includes the session's correlation identifier, sourced from the Scoped Value described in `00 §15`, never passed as an explicit parameter threaded through unrelated method signatures.

---

## 12. Test Code Style

- Test method names follow `unitUnderTest_condition_expectedOutcome` (§4), stating the scenario precisely enough that a failing test name alone tells a reader what broke.
- Arrange-Act-Assert sections are separated by a single blank line each; for short, self-evident tests, the blank-line separation alone is sufficient and explicit `// Arrange` / `// Act` / `// Assert` labels are not required — for longer tests where the sections aren't visually obvious, the labels are added.
- Assertions use JUnit 5's built-in `Assertions` methods exclusively — no additional assertion library (e.g., AssertJ, Hamcrest) is introduced, consistent with `00 §16`'s closed dependency policy; a genuine need for fluent assertions is grounds for an ADR, not a quiet addition to a test's imports.
- Mockito mocks are declared and configured within the test method that uses them, or in a `@BeforeEach` setup method shared across a small, cohesive group of tests in the same class — never as class-level fields shared across unrelated test methods.

---

## 13. Static Analysis and Formatter Tooling

The full enforcement chain, run as part of `mvn verify` (`00 §36`) and therefore blocking on every merge:

| Tool | Enforces |
|---|---|
| Spotless (Palantir Java Format) | Formatting: indentation, brace placement, import grouping (§2, §3). |
| Checkstyle | Structural rules not purely about whitespace: import order, member order (§5), JavaDoc presence (§8). |
| PMD | Size and complexity metrics from `00 §10`: cyclomatic complexity ≤ 10, method length ≤ 40 lines, class length ≤ 400 lines. |
| SpotBugs | Correctness and null-safety static analysis, complementing §7's manual rules with automated detection. |
| ArchUnit | Architectural boundary rules defined in `04 §8` — this is layout enforcement, not style enforcement, but runs in the same `mvn verify` pass. |

All five are configured to **fail** the build on violation. None run in "warning-only" mode — a warning that nobody is required to act on is not a quality gate, per `00 §10`'s zero-tolerance framing.

---

## 14. Commit Message Convention

Commit messages follow the Conventional Commits format: `type(scope): description`, where `type` is one of `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, or `perf`, and `scope` names the affected feature package from `04 §4` (e.g., `feat(estimation): add CHROM algorithm implementation`). Where a commit completes or advances a specific item in `15_TASK.md`, the task identifier is referenced in the commit body, not the subject line, to keep subject lines scannable.

---

## 15. Relationship to Other Documents

| Document | What It Inherits From This Document |
|---|---|
| `14_DEPLOYMENT.md` | The CI build pipeline runs every tool listed in §13 as a blocking step. |
| `15_TASK.md` | Task entries are referenced from commits per §14's convention, and from `TODO` comments per §9. |

---

## 16. Revision History

| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-07-12 | Initial ratified version, derived from `00_MASTER_PROMPT.md` v1.0.0 and `04_PACKAGE_STRUCTURE.md` v1.0.0. |

---

*End of 05_CODING_STANDARD.md. Subordinate to `00_MASTER_PROMPT.md`; binding on all documents listed in §15.*