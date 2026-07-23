/// Domain contracts and value objects for face-region detection within a frame.
///
/// Owns the [InferenceEngine] port and the `RegionOfInterest` value object. Per
/// `00 §22.2` the normal absence of a face is modelled as a typed result, never an
/// exception. Framework-free per `00 §9`: no ONNX Runtime or MediaPipe type appears
/// here — the inference adapter lives in `infrastructure.inference.onnx`. Governed by
/// `03_ARCHITECTURE.md §4` and `09_AI_INTEGRATION.md`.
package id.asr.rppgvitals.domain.detection;
