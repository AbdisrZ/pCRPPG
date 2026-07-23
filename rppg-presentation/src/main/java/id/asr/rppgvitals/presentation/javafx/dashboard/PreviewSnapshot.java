package id.asr.rppgvitals.presentation.javafx.dashboard;

import java.util.Objects;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;

/// The latest camera frame paired with the ROI detected within it, for the live preview overlay
/// (`06_UI_GUIDELINE.md §6.2`).
///
/// A transient, in-memory view value carried from the processing pipeline to the View; never persisted
/// (`02_SOFTWARE_REQUIREMENT.md` DR-1). The [Frame] is already an immutable value (it defensively copies
/// its buffer), so this record simply groups it with its region.
///
/// @param frame the frame to display; never `null`
/// @param roi the region of interest to outline, or `null` when none was detected
public record PreviewSnapshot(Frame frame, RegionOfInterest roi) {

    /// Validates the snapshot.
    public PreviewSnapshot {
        Objects.requireNonNull(frame, "frame");
    }
}
