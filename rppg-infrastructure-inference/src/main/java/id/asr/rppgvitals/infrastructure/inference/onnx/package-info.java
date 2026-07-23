/// ONNX Runtime adapter implementing the domain `InferenceEngine` port.
///
/// Single-responsibility infrastructure package (`04 §3`): `OnnxInferenceEngine` runs the
/// face/landmark model through ONNX Runtime and derives a `RegionOfInterest`, returning a typed
/// "no face" result rather than throwing for the normal empty case (`00 §22.2`). ONNX Runtime
/// error types are translated into `ModelInferenceException` at this boundary; no ONNX type
/// escapes inward. Governed by `03_ARCHITECTURE.md §6.3` and `09_AI_INTEGRATION.md §5–§6`.
package id.asr.rppgvitals.infrastructure.inference.onnx;
