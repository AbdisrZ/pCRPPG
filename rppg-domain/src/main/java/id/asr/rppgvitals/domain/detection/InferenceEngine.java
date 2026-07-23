package id.asr.rppgvitals.domain.detection;

import java.util.Optional;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.exception.ModelInferenceException;

/// Domain port for face-region detection (`03_ARCHITECTURE.md §4`).
///
/// Given a [Frame], an `InferenceEngine` returns the detected face [RegionOfInterest], or an empty
/// result when no face is present. The normal absence of a face is modelled as data — an empty
/// [Optional] — never an exception (`00_MASTER_PROMPT.md §22.2`). Implemented in the infrastructure
/// layer by `OnnxInferenceEngine` (`09_AI_INTEGRATION.md`).
///
/// **Thread-safety.** Implementations wrap a stateful native inference session and are not required
/// to be thread-safe; a single owning thread drives detection per the threading model of
/// `11_THREADING.md`.
public interface InferenceEngine {

    /// Detects the primary face region within the given frame.
    ///
    /// @param frame the frame to analyse; never `null`
    /// @return the detected region, or [Optional#empty()] when no face is present in the frame
    /// @throws ModelInferenceException if the underlying model fails to run
    Optional<RegionOfInterest> detectRegionOfInterest(Frame frame);
}
