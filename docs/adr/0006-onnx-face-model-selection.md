# ADR 0006 — ONNX Face/Landmark Model Selection (direction)

- **Status:** Accepted (direction); empirical validation pending
- **Date:** 2026-07-23
- **Decision process:** `00_MASTER_PROMPT.md §8`. Resolves the *direction* of NC-001; the exact
  artifact + license pin remains a validation step (`09_AI_INTEGRATION.md §3`).
- **Relates to:** T-202 (select and validate an ONNX face/landmark model).

## Context

`09_AI_INTEGRATION.md §2` already **binds** the integration strategy to Option (a): ONNX-format models
derived from MediaPipe's face detection (BlazeFace) and face landmark (Face Mesh) models, run through
ONNX Runtime's Java API in-process. `09 §3` deliberately does **not** pin a specific model file —
community TFLite→ONNX conversions are informally maintained, so that choice is an implementation task
with validation criteria attached. The project architect confirmed the direction (2026-07-23):
**Google MediaPipe**, with the **ubicomplab rPPG-Toolbox** (https://github.com/ubicomplab/rPPG-Toolbox)
available as a supporting resource.

## Decision

1. **Model family: MediaPipe BlazeFace (face detection) + Face Mesh (face landmark), consumed as
   ONNX**, exactly the family `09 §2` binds. This gives a face bounding box plus a dense landmark set
   from which the forehead/cheek ROI of `07 §4` is derived (`09 §6`).
2. **Sourcing preference (`09 §3`):** adopt a pre-converted **end-to-end ONNX** artifact (anchor
   decoding + non-maximum suppression baked into the graph) from a reputable community conversion,
   rather than performing a fresh TFLite→ONNX conversion in-house — MediaPipe Face Mesh has known
   op-compatibility pitfalls in conversion (`09 §4`), so reusing an already-solved conversion is the
   lower-risk path. If no adequately-licensed pre-converted artifact is found at validation time, the
   fallbacks of `09 §4` apply (in-house conversion with op-debugging budget, or revisiting Option (b))
   via `00 §8`.
3. **rPPG-Toolbox is a reference, not the face model.** It is a Python research toolbox implementing
   rPPG methods (including CHROM/POS, which this project already implements independently in
   `domain.estimation`). Its role here is (a) cross-validating our estimator outputs and (b) a
   candidate source of recorded signals/ground-truth for the golden-file fixtures of **T-109** — not
   as the ONNX face/landmark model. Its license is checked separately if any of its data or code is
   actually vendored.

## What remains before T-202 is Done (empirical validation, `09 §3`)

Deferred because it needs network access to fetch the artifact and reference data, and dovetails with
`OnnxInferenceEngine` (T-203):

- Pin the **exact artifact** (repository, file, and a content hash) and verify its **license** and the
  converter's license are compatible (`00 §16`).
- **Validate** the model's output against a small set of reference images with known face positions.
- Record the **landmark indices** used for the forehead/cheek ROI (`09 §6`) alongside the pinned model.
- Measure **inference latency** against the per-frame budget (`12_PERFORMANCE.md`) on reference
  hardware — not assumed from model size.

## Consequences

- The strategic ambiguity of NC-001 is resolved (MediaPipe BlazeFace + Face Mesh via ONNX); the
  remaining open item is the concrete artifact pin + validation, which stays with T-202/T-203.
- No code or build change results from this ADR; it is a recorded decision only.
