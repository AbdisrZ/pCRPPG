package id.asr.rppgvitals.bootstrap;

import java.util.Objects;
import java.util.Optional;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.InferenceEngine;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;

/// A placeholder [InferenceEngine] that reports the central region of each frame as the ROI, so the
/// live pipeline runs end-to-end before the real ONNX face detector (T-203) is wired in.
///
/// It is not face detection — it assumes the subject is roughly centred and returns the middle portion
/// of the frame at a fixed confidence. It lives in the composition root as a temporary stand-in and is
/// replaced by `OnnxInferenceEngine` once a model is adopted (`09_AI_INTEGRATION.md`, NC-001).
public final class HeuristicInferenceEngine implements InferenceEngine {

    private static final double REGION_FRACTION = 0.5;
    private static final double HEURISTIC_CONFIDENCE = 0.8;

    /// Creates the placeholder engine.
    public HeuristicInferenceEngine() {}

    /// {@inheritDoc}
    @Override
    public Optional<RegionOfInterest> detectRegionOfInterest(Frame frame) {
        Objects.requireNonNull(frame, "frame");
        int width = Math.max(1, (int) Math.round(frame.width() * REGION_FRACTION));
        int height = Math.max(1, (int) Math.round(frame.height() * REGION_FRACTION));
        int x = (frame.width() - width) / 2;
        int y = (frame.height() - height) / 2;
        return Optional.of(new RegionOfInterest(x, y, width, height, HEURISTIC_CONFIDENCE));
    }
}
