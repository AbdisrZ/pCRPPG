# 12_PERFORMANCE.md
# Performance Engineering
## rPPG Desktop Vitals Monitor

---

**Document Control**

| Field | Value |
|---|---|
| Document ID | PRF-12 |
| Version | 1.0.0 |
| Status | **BINDING** — Measurement and Budget Specification |
| Depends On | `00_MASTER_PROMPT.md` (§11, §32), `07_SIGNAL_PROCESSING.md`, `08_ESTIMATOR_ENGINE.md`, `09_AI_INTEGRATION.md`, `11_THREADING.md` |
| Consumed By | `13_TESTING.md`, `14_DEPLOYMENT.md` |
| Precedence | Subordinate to `00_MASTER_PROMPT.md §11`. The targets there are fixed; this document only decides how they are apportioned across components and how compliance is measured. |
| Maintainer | Human Project Architect — Abdi Soleh Rosadi |
| Last Updated | 2026-07-12 |

---

## 1. Purpose of This Document

`00_MASTER_PROMPT.md §11` states five system-level targets — camera-to-display latency, per-frame processing budget, cold start, memory footprint, convergence time — as single numbers. A single number does not tell an implementer how much of the 33 ms per-frame budget belongs to face detection versus signal extraction, or whether the 3-second cold start has any margin in it at all. This document apportions each system-level target across the specific components named in `07`, `08`, `09`, and `11`, and defines how compliance is actually measured rather than assumed.

Every budget figure below is a **planning target, not a guarantee** — `00 §32`'s "measure, then optimize" rule applies to this document as much as to any code change. Where this document states a number without an empirical measurement behind it yet, that is stated explicitly, not disguised as fact.

---

## 2. Reference Hardware Profile

Benchmarks in this document and in `13_TESTING.md`'s CI gate are run against machines meeting this **reference class**, not one specific pinned SKU — a mid-range consumer laptop released within roughly the last four years, quad-core x86-64 CPU, no dedicated GPU (CPU-only inference, consistent with `09_AI_INTEGRATION.md`'s ONNX Runtime CPU execution provider), 8–16 GB RAM, and a standard integrated or USB UVC webcam. Every recorded benchmark run logs the exact machine it was measured on alongside the result — the reference *class* is what the targets in `00 §11` are meant to be achievable on; the exact machine used for a given measurement is data, not policy.

---

## 3. Per-Frame Processing Budget

Apportioning `00 §11`'s ≤ 33 ms/frame sustained target across the work that genuinely happens every frame (`11 §3`'s `processingExecutor` loop):

| Component | Budget | Source |
|---|---|---|
| Spatial averaging within ROI (`07 §4`) | ≤ 2 ms | Cheap: a mean over a bounded pixel region. |
| `InferenceEngine.detectRegionOfInterest` (`09 §5`) | ≤ 20 ms | The dominant cost — CPU-only ONNX inference on a lightweight detection/landmark model. This is the figure most likely to need real measurement before it can be trusted (§8). |
| Per-frame algorithm combination math (POS/Green-channel step per `07 §5`'s per-frame stepping) | ≤ 2 ms | A handful of vector operations over the current window — not the FFT, which is budgeted separately (§7). |
| Queue and orchestration overhead (`11 §4`) | ≤ 2 ms | Enqueue/dequeue and loop bookkeeping. |
| **Margin** | ~7 ms | Deliberately retained, not allocated — the first place profiling effort goes if any of the above runs over. |

CHROM's own combination math (`07 §7`) runs on its 0.8-second window step rather than every frame, and is covered by the same per-frame line item only on the frames where it actually executes; it is not a separate, additive cost on every frame.

---

## 4. Camera-to-Display Latency Budget

Apportioning `00 §11`'s ≤ 100 ms target:

| Stage | Typical | Worst Case |
|---|---|---|
| Camera driver capture (hardware/OS-dependent, not fully controllable by this application) | ~10 ms | ~15 ms |
| Processing (§3) | ~15–20 ms (rarely the full 33 ms budget in steady state) | 33 ms |
| Capture→processing queue wait (`11 §4`) | ~0 ms — the pipeline keeps pace under normal load | Up to ~100 ms if backpressure engages; this is the ceiling the queue depth in `11 §4` was specifically chosen to enforce, not a typical value |
| UI marshal (`Platform.runLater` dispatch + JavaFX render, `11 §9`) | ~5–10 ms | ~15 ms |
| **Typical total** | **~30–40 ms**, comfortably inside the 100 ms target | — |

The worst-case column is not additive with the typical column — the queue-wait worst case only materializes when the system is already degraded (falling behind), at which point the drop-oldest policy (`11 §4`) is actively bounding the damage rather than the system operating normally with all four stages simultaneously at their individual worst case.

---

## 5. Memory Budget

Apportioning `00 §11`'s ≤ 512 MB heap target (native OpenCV/ONNX Runtime memory is tracked separately, per that section):

| Component | Estimated Budget |
|---|---|
| JVM baseline (class loading, JIT compilation structures, GC bookkeeping) | ~100–150 MB |
| JavaFX framework overhead | ~100 MB |
| ONNX Runtime session and loaded model weights (`09 §7`) | ~50–150 MB, depending on the model selected per `09 §3` |
| Pipeline buffers: capture queue (`11 §4`), in-session sample buffer (`10 §6`), waveform windows (`07 §5`) | ~10–20 MB |
| SQLite JDBC connection and driver overhead (`10 §7`) | ~10 MB |
| Application code, remaining margin | Remainder |

This table is explicitly a planning estimate. The ONNX Runtime line in particular cannot be pinned precisely until `09 §3`'s model selection is finalized, and the whole table is to be validated against Java Flight Recorder heap sampling (§8) during a sustained session on reference hardware (§2) before being treated as confirmed rather than estimated.

---

## 6. Cold-Start Budget

Apportioning `00 §11`'s ≤ 3 s target from launch to first live preview frame:

| Stage | Estimated Duration |
|---|---|
| JVM startup and class loading | ~0.5–1.0 s |
| ONNX Runtime session creation and model load (`09 §7`, a one-time cost) | ~0.5–1.0 s |
| SQLite connection and schema-version check (`10 §5`) | ~0.1–0.2 s |
| Camera device enumeration and first frame acquisition | ~0.5–1.0 s — camera driver initialization is a known source of variance across hardware and is the least controllable line in this table |
| JavaFX scene construction and first render | ~0.3–0.5 s |
| **Estimated total** | **~1.9–3.7 s** |

Unlike §4's latency budget, this one has **little to no comfortable margin** against the 3-second target — the estimated range already brushes against or exceeds it. This is flagged deliberately: cold-start performance is the single target in `00 §11` most likely to need early, dedicated profiling attention rather than being assumed safe until late in development, and `15_TASK.md` should schedule that profiling early rather than as a pre-release afterthought.

---

## 7. Estimation Cadence Budget

The once-per-second FFT-based estimation step (`08 §2`) is not part of the per-frame budget in §3 — it runs on its own one-second cadence and simply needs to complete comfortably within that window. A 1,024-point FFT (`08 §2`) via Apache Commons Math is expected to complete in low single-digit milliseconds on the reference hardware class (§2), leaving very wide margin within its one-second budget; this is not expected to be a bottleneck and is not allocated further scrutiny in this document beyond confirming that expectation empirically once implemented (§8).

---

## 8. Benchmark Methodology

Per `00 §32`, distinct measurement techniques for distinct kinds of claims:

| What | Method |
|---|---|
| Hot-path component cost (each `SignalEstimator` implementation's per-window computation, the FFT step, a single `InferenceEngine` call) | JMH microbenchmarks — never ad hoc `System.nanoTime()` calls left in production code. |
| End-to-end camera-to-display latency (§4) | A dedicated, instrumented integration test harness that timestamps each pipeline stage boundary across the real thread topology (`11 §3`) — JMH's single-threaded model does not fit a measurement that inherently spans `captureExecutor`, `processingExecutor`, and the JavaFX Application Thread. |
| Memory footprint (§5) | Java Flight Recorder heap sampling during a sustained, multi-minute session on reference hardware (§2) — no additional profiling dependency is required, since JFR ships with the JDK. |
| Cold start (§6) | Wall-clock measurement from process launch to first rendered live-preview frame, averaged over multiple cold launches, not a single run. |

---

## 9. Regression Tracking and CI Gating

Every benchmark run's result is recorded (a simple committed results log, not a new dependency) alongside the exact reference-class machine it ran on (§2). A subsequent run that regresses any figure in §3–§7 by more than 10% against the last recorded baseline is treated as a build-blocking failure under `00 §36`'s Build Verification Protocol, not a warning to note and move past — consistent with `00 §32`'s rule that every accepted optimization (and, symmetrically, every accepted regression) is recorded with its measurement, not asserted from memory.

---

## 10. Relationship to Other Documents

| Document | What It Inherits From This Document |
|---|---|
| `13_TESTING.md` | The benchmark methodology in §8 and the regression-gating rule in §9 are the concrete CI behavior that document's tooling configuration implements. |
| `14_DEPLOYMENT.md` | The cold-start budget in §6 is the acceptance bar the packaged, installed artifact must meet — not just a development-environment measurement. |

---

## 11. Revision History

| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-07-12 | Initial ratified version, apportioning `00_MASTER_PROMPT.md §11`'s targets across `07`, `08`, `09`, and `11`, all v1.0.0. |

---

*End of 12_PERFORMANCE.md. Subordinate to `00_MASTER_PROMPT.md §11`; binding on all documents listed in §10.*