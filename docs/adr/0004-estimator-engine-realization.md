# ADR 0004 — Estimator Engine Realization (Filter, FFT, Estimators)

- **Status:** Accepted
- **Date:** 2026-07-22
- **Decision process:** `00_MASTER_PROMPT.md §8`, node G for the realization choices; the deferred
  items (NC-002, overlap-add, full confidence) are recorded, not silently resolved.
- **Implements:** T-104 (Green), T-105 (CHROM), T-106 (POS), T-107 (FFT + confidence), all in
  `rppg-domain` (`07_SIGNAL_PROCESSING.md §6–§9`, `08_ESTIMATOR_ENGINE.md §2–§3`).

## Context

`07`/`08` specify the estimation mathematics but leave several realization details to the
implementer: how to realize the "zero-phase 3rd-order Butterworth bandpass" (Commons Math has no
filter design), the exact overlap-add reconstruction, and the confidence-component weights (NC-002).
All decisions below are validated by a green `./mvnw -P ci clean verify`: 99 domain unit tests
including synthetic-signal recovery of a 72 bpm pulse by all three estimators, and the domain
coverage gate at ≥ 90% line / 85% branch.

## Decisions

1. **Butterworth realized by a general designer, not hardcoded coefficients.** `ButterworthBandpassFilter`
   designs the digital transfer function at construction from an analog Butterworth low-pass prototype
   via the low-pass-to-bandpass and bilinear transforms (the `scipy.signal.butter` path), using
   Commons Math `Complex` for the pole/zero algebra. A validated designer is more trustworthy than
   magic numbers that cannot be independently checked; five filter-response tests confirm unity
   passband gain at 1.2 Hz, strong attenuation at 0.1 Hz and 6 Hz, and zero phase.
2. **Zero-phase via padded filtfilt.** Forward-then-reverse filtering, with odd-reflection edge
   padding (as `scipy.signal.filtfilt`). Without the padding, the startup transient of a finite
   window injects spurious low-frequency energy near the 0.7 Hz edge — this actually broke the
   Green estimator's first test run (it locked onto ~0.7 Hz); the padding fixed it.
3. **Hann window before the FFT.** `08 §2` says "zero-pad then FFT"; taken literally, the
   zero-padding boundary discontinuity leaks broadband energy that swamps a small (~1%) pulse. A Hann
   window tapers the ends to zero and removes it — the same Hanning weighting `07 §9` already uses for
   reconstruction. A justified, standard leakage-reduction refinement.
4. **Full-window processing, not per-sub-window overlap-add (V1 simplification).** `07 §5/§9`
   describe 1.6 s sub-windows combined by Hanning overlap-add; POS and Green use a ~1-frame step,
   which is effectively continuous whole-window processing, and computing CHROM/POS over the whole
   analysis window is a well-understood variant. The exact overlap-add index math is unspecified in
   `07`; adding that machinery without golden-file data to validate it against would be complexity
   ahead of measurement (`00 §32`). Deferred to T-109 (golden fixtures); revisit if it proves
   insufficient.
5. **The estimator computes only the spectral-SNR confidence component; the full three-component
   score composes at the orchestrator (T-302).** `08 §3`'s temporal-consistency (needs the prior
   window's bpm) and ROI-stability (needs the `RegionOfInterest` stream) components require
   session-level context that a single `PpgWaveform` — the estimator's only input — does not carry.
   The estimator therefore produces the intrinsic spectral confidence (`snr / (1 + snr)`); the
   orchestrator, which owns history and the ROI stream, folds in the other two components. **NC-002
   (the component weights) stays deferred** and now belongs to T-302, not the estimator.
6. **`SignalQualityException` on degenerate windows** (`08 §5`): a zero denominator in the CHROM/POS
   alpha step, a zero channel mean during normalisation, a zero-variance Green window, or an
   empty-band spectrum. Routine low quality remains data (low confidence), never an exception.
7. **Internal collaborators are package-private** — `ButterworthBandpassFilter`,
   `PulseSpectrumAnalyzer`, `EstimatorSupport` are implementation details of the estimators, composed
   in (not inherited — `00 §5`), and are not public components in `04 §4`'s tree.

## Consequences

- All three estimators recover a clean synthetic pulse to within ~2 bpm at high confidence; CHROM is
  the V1 default (`08 §6`). The three share one filter, one analyzer, and one math helper.
- Two items are explicitly carried forward: NC-002 weights and the full confidence composition (both
  now at T-302), and the overlap-add refinement (at/after T-109). T-108 (`SignalQuality` transitions)
  and T-109 (golden fixtures) remain open in Phase 1.
