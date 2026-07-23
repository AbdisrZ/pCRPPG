package id.asr.rppgvitals.application.usecase.measurement;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;
import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;
import id.asr.rppgvitals.domain.signal.SignalQuality;

/// The callback contract through which the `LiveMeasurementOrchestrator` delivers live results
/// (`03_ARCHITECTURE.md §6.2`).
///
/// It is a plain application-owned interface — never a JavaFX type — so the Dependency Rule holds
/// (`00_MASTER_PROMPT.md §9`). Callbacks are invoked from the processing thread (`11_THREADING.md §9`);
/// the presentation-layer implementation marshals each one onto the JavaFX Application Thread with
/// `Platform.runLater` before touching any UI state, and must not block.
public interface MeasurementObserver {

    /// Called once per estimation update with a newly produced heart-rate estimate.
    ///
    /// @param estimate the latest estimate; never `null`
    void onHeartRateUpdated(HeartRateEstimate estimate);

    /// Called when the live signal-quality state transitions between searching and stable.
    ///
    /// @param quality the new signal-quality state; never `null`
    void onSignalQualityChanged(SignalQuality quality);

    /// Called when the session degrades due to an operational condition (camera lost, poor lighting,
    /// ROI lost) — `03_ARCHITECTURE.md §7.2`.
    ///
    /// @param reason a short, human-oriented description of what degraded the session; never `null`
    void onSessionDegraded(String reason);

    /// Called when a previously degrading condition is resolved and acquisition resumes.
    void onSessionRecovered();

    /// Called once per captured frame with the frame and the ROI detected within it, for the live
    /// camera preview and its bounding-box overlay (`06_UI_GUIDELINE.md §6.2`). This is a transient,
    /// in-memory display signal only — it is never persisted (`02_SOFTWARE_REQUIREMENT.md` DR-1). The
    /// default is a no-op so observers that do not render a preview need not implement it.
    ///
    /// @param frame the captured frame; never `null`
    /// @param roi the region of interest detected in the frame, or `null` if none was detected
    default void onPreviewFrame(Frame frame, RegionOfInterest roi) {
        // No-op by default: only the live ViewModel renders the preview.
    }
}
