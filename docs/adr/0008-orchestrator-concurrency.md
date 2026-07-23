# ADR 0008 — LiveMeasurementOrchestrator Concurrency Realization

- **Status:** Accepted
- **Date:** 2026-07-23
- **Decision process:** `00_MASTER_PROMPT.md §8`, node G. Realises `11_THREADING.md`.
- **Implements:** T-302 (threaded orchestrator), and folds in T-303 (capture→processing executor
  topology + bounded queue) and T-304 (scoped-value correlation id).

## Context

ADR 0007 delivered the deterministic `MeasurementPipeline` and anticipated the threaded wrapper would
carry camera-loss/recovery to the processing thread as *queued control events*. Building it, a simpler
and safer design emerged. Validated by a green `./mvnw -P ci clean verify`, including three
deterministic concurrency tests (latch-awaited, no sleep): happy-path estimate + device close on stop,
start-while-running rejection, and camera-loss → recovery notification.

## Decisions

1. **Two virtual-thread executors** (`captureExecutor`, `processingExecutor`), each running one
   long-running task per session (`11 §2`), created in the constructor and shut down deterministically
   on `close()` — `shutdown()` → bounded `awaitTermination` → `shutdownNow()` (`11 §8`). The
   orchestrator is `AutoCloseable`.
2. **Bounded drop-oldest queue** — `ArrayBlockingQueue<Frame>` depth 3; the single producer drops the
   oldest on full (`while (!offer) poll()`), never blocking capture (`11 §4`).
3. **Degradation bridge is a `volatile` reason, not queued control events** (a deviation from ADR
   0007's anticipated approach). Mixing control events into the drop-oldest frame queue would risk
   dropping a control event; instead the capture thread publishes a `volatile String degradationReason`,
   and the processing thread reads it each loop and dispatches `onDegraded`/`onRecovered` to the
   pipeline. This preserves the confinement of the pipeline to the processing thread (`11 §5`) with
   less machinery.
4. **Cooperative stop (`11 §7`).** `stop()` clears `running`, cancels (interrupts) the capture task,
   then lets the processing task drain the queue and exit on its own (never interrupted mid-frame),
   and releases the device. The processing loop uses a bounded `poll(timeout)` so it also wakes to
   observe degradation while idle. A cancelled task's `CancellationException` from `get()` is expected
   and swallowed.
5. **Scoped value (`11 §6`, T-304).** `CORRELATION_ID` is declared once here; each loop binds it at its
   own start via `ScopedValue.where(...).run(...)`, since a binding does not cross executor boundaries
   without structured concurrency (deferred, `00 §27`).
6. **Reconnect** on camera loss polls `isDeviceAvailable()` with a bounded `parkNanos` interval,
   interruptible via the cooperative-cancellation check (`03 §7.2`).

## Deferred

- **`persistenceExecutor` (`11 §2`).** Persistence is currently synchronous through the
  `MeasurementRepository`/use cases; the end-of-session write (`11 §7.4`) happens via
  `EndMeasurementSessionUseCase`. Running persistence ops on a dedicated executor (to keep them off the
  UI thread) is a composition-root concern, deferred to T-501.
- **Wiring `Start`/`EndMeasurementSessionUseCase` to `start()`/`stop()`** (`03 §6.2`) is a
  composition-root concern, also T-501; the orchestrator is independently usable and tested now.
