/// OpenCV adapter implementing the domain `FrameSource` port.
///
/// Single-responsibility infrastructure package (`04 §3`): `OpenCvFrameSource` enumerates and
/// opens camera devices via the OpenCV Java bindings, yields `Frame`s, and releases the native
/// handle deterministically. All OpenCV checked/native error conditions are translated into the
/// domain exception hierarchy at this boundary (`00 §22.1`); no OpenCV type escapes inward.
/// Governed by `03_ARCHITECTURE.md §6.3`.
package id.asr.rppgvitals.infrastructure.capture.opencv;
