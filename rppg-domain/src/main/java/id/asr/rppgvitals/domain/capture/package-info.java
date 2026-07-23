/// Domain contracts and value objects for raw camera frame capture.
///
/// Owns the [FrameSource] port together with the `Frame` and `CaptureConfiguration`
/// value objects that cross the capture boundary. This package is framework-free per
/// `00 §9`: no `org.opencv.*` or other infrastructure type is referenced here — the
/// OpenCV adapter lives in `infrastructure.capture.opencv` and depends inward on this
/// contract. Governed by `03_ARCHITECTURE.md §4` and `04_PACKAGE_STRUCTURE.md §4`.
package id.asr.rppgvitals.domain.capture;
