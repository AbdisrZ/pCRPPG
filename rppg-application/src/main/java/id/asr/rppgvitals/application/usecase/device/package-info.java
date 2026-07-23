/// Application use case for enumerating available camera devices.
///
/// Holds `ListAvailableCameraDevicesUseCase`, which surfaces the capture devices offered by
/// the domain `FrameSource` port so the presentation layer can let the user pick one before a
/// session starts. Depends only on `rppg-domain`; never on `javafx.*`. Governed by
/// `03_ARCHITECTURE.md §6.2` and `02_SOFTWARE_REQUIREMENT.md` (FR-501, FR-502).
package id.asr.rppgvitals.application.usecase.device;
