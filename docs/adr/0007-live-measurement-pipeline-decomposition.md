# ADR 0007 — Live-Measurement Pipeline Decomposition & Canonical Frame Format

- **Status:** Accepted
- **Date:** 2026-07-23
- **Decision process:** `00_MASTER_PROMPT.md §8`, node G. Refines a T-101 type (`Frame`) per `00 §20.2`.
- **Implements:** T-302 (part 1 — `MeasurementObserver` + deterministic pipeline core). The threaded
  `LiveMeasurementOrchestrator` (T-302 part 2, + T-303 topology, + T-304 scoped values) remains.

## Context

`03 §6.2`/`11` describe `LiveMeasurementOrchestrator` as a streaming, multi-threaded pipeline. Two
gaps surfaced building it: (a) `07 §4`'s spatial-averaging step (Frame + ROI → `PpgSample`) is named
in the signal doc but not as a `03 §4` port; (b) `Frame`'s pixel layout was declared "adapter-native,"
which a domain averager cannot interpret. Both are resolved here, and the deterministic processing
logic is extracted so it is unit-testable without concurrency. Validated by a green
`./mvnw -P ci clean verify`.

## Decisions

1. **Canonical `Frame` pixel format.** `Frame` now specifies 8-bit **RGB**, 3 channels, interleaved,
   row-major, with the invariant `pixels.length == width * height * 3` (`Frame.CHANNELS = 3`). The
   **capture adapter converts** its native layout (e.g. OpenCV BGR) into this canonical form when
   constructing a `Frame`. This lets domain-side spatial averaging read the buffer without knowing the
   backend, resolving the previous "adapter-native format" contradiction.
2. **`RoiSpatialAverager` (domain service, `domain.signal`).** Implements `07 §4`: mean R/G/B over the
   ROI (clamped to frame bounds) → one `PpgSample`. It is the domain computation the orchestrator
   *uses* but does not itself perform (`00 §9`). A supporting type not itemised in `04 §4`'s tree,
   analogous to the estimator helpers.
3. **`MeasurementObserver` (application contract).** The plain callback interface of `03 §6.2`
   (`onHeartRateUpdated`, `onSignalQualityChanged`, `onSessionDegraded`, `onSessionRecovered`) — never a
   JavaFX type. Callbacks fire on the processing thread; the ViewModel marshals via `Platform.runLater`
   (`11 §9`).
4. **`MeasurementPipeline` (package-private, application).** The single-threaded processing-loop body
   (`11 §3`): per frame → detect ROI → average → rolling analysis window → every interval run the
   `SignalEstimator`, drive the `SignalQualityStateMachine`, notify the observer. Extracted from the
   orchestrator so the substantive logic is deterministically testable without threads (`00 §5`), and
   confined to the single processing task that owns it (`11 §5`). A `SignalQualityException` from a
   degenerate window is caught and the estimate skipped — not a session failure (`08 §5`, `02 NFR-401`).

## Consequences

- The deterministic heart of the live pipeline exists and is fully unit-tested; `FrameTest` was updated
  for the new invariant.
- The remaining threaded `LiveMeasurementOrchestrator` will *wrap* `MeasurementPipeline`: capture and
  processing run on separate virtual-thread executors across a bounded drop-oldest queue (`11 §2/§4`),
  and — because the pipeline is confined to the processing thread — camera-loss/recovery detected on the
  capture thread will be delivered to it as queued control events, keeping the confinement of decision 4
  intact.
- The `Frame` canonical-format decision creates a concrete obligation on the future `OpenCvFrameSource`
  (T-201): convert BGR→RGB when building a `Frame`.
