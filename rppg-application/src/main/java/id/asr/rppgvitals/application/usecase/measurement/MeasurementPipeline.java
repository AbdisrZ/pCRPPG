package id.asr.rppgvitals.application.usecase.measurement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.InferenceEngine;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;
import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;
import id.asr.rppgvitals.domain.estimation.SignalEstimator;
import id.asr.rppgvitals.domain.exception.SignalQualityException;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.signal.PpgSample;
import id.asr.rppgvitals.domain.signal.PpgWaveform;
import id.asr.rppgvitals.domain.signal.RoiSpatialAverager;
import id.asr.rppgvitals.domain.signal.SignalQuality;
import id.asr.rppgvitals.domain.signal.SignalQualityStateMachine;

/// The single-threaded processing-loop body of the `LiveMeasurementOrchestrator` (`03_ARCHITECTURE.md
/// §6.2`, `11_THREADING.md §3`): for each frame it detects the ROI, spatially averages it into a
/// [PpgSample], maintains a rolling analysis window, and — every estimation interval — runs the
/// [SignalEstimator] and drives the [SignalQualityStateMachine], notifying the [MeasurementObserver].
///
/// Extracted from the threaded orchestrator so this logic is deterministically unit-testable without
/// concurrency (`00_MASTER_PROMPT.md §5`). Package-private, not thread-safe: it is confined to the
/// single processing task that owns it (`11 §5`).
final class MeasurementPipeline {

    private final InferenceEngine inferenceEngine;
    private final SignalEstimator estimator;
    private final MeasurementObserver observer;
    private final MeasurementSession session;
    private final int analysisWindowSamples;
    private final int estimateIntervalFrames;
    private final double samplingRateHz;

    private final RoiSpatialAverager averager = new RoiSpatialAverager();
    private final SignalQualityStateMachine stateMachine = new SignalQualityStateMachine();
    private final Deque<PpgSample> window = new ArrayDeque<>();

    private SignalQuality currentQuality = stateMachine.initial();
    private int framesSinceEstimate;

    MeasurementPipeline(
            InferenceEngine inferenceEngine,
            SignalEstimator estimator,
            MeasurementObserver observer,
            MeasurementSession session,
            int analysisWindowSamples,
            int estimateIntervalFrames,
            double samplingRateHz) {
        this.inferenceEngine = Objects.requireNonNull(inferenceEngine, "inferenceEngine");
        this.estimator = Objects.requireNonNull(estimator, "estimator");
        this.observer = Objects.requireNonNull(observer, "observer");
        this.session = Objects.requireNonNull(session, "session");
        this.analysisWindowSamples = requirePositive(analysisWindowSamples, "analysisWindowSamples");
        this.estimateIntervalFrames = requirePositive(estimateIntervalFrames, "estimateIntervalFrames");
        if (!(samplingRateHz > 0.0)) {
            throw new IllegalArgumentException("samplingRateHz must be positive, was " + samplingRateHz);
        }
        this.samplingRateHz = samplingRateHz;
    }

    /// Processes one captured frame, emitting an estimate when the interval elapses.
    ///
    /// @param frame the captured frame to process; never `null`
    void onFrame(Frame frame) {
        Objects.requireNonNull(frame, "frame");
        Optional<RegionOfInterest> roi = inferenceEngine.detectRegionOfInterest(frame);
        roi.ifPresent(region -> addSample(averager.sample(frame, region)));

        if (++framesSinceEstimate >= estimateIntervalFrames) {
            framesSinceEstimate = 0;
            emitEstimate();
        }
    }

    /// Moves the session into the degraded state and notifies the observer (`11 §7`, `03 §7.2`).
    ///
    /// @param reason a short, human-oriented description of the degrading condition; never `null`
    void onDegraded(String reason) {
        Objects.requireNonNull(reason, "reason");
        currentQuality = stateMachine.afterDegradingCondition(reason);
        observer.onSessionDegraded(reason);
    }

    /// Leaves the degraded state once the condition is resolved and notifies the observer.
    void onRecovered() {
        currentQuality = stateMachine.afterConditionResolved(currentQuality);
        observer.onSessionRecovered();
    }

    private void addSample(PpgSample sample) {
        window.addLast(sample);
        if (window.size() > analysisWindowSamples) {
            window.removeFirst();
        }
    }

    private void emitEstimate() {
        boolean windowComplete = window.size() >= analysisWindowSamples;
        if (!windowComplete) {
            transitionQuality(stateMachine.afterEstimate(currentQuality, 0.0, false));
            return;
        }
        try {
            HeartRateEstimate estimate = estimator.estimate(new PpgWaveform(new ArrayList<>(window), samplingRateHz));
            session.recordEstimate(estimate);
            observer.onHeartRateUpdated(estimate);
            transitionQuality(stateMachine.afterEstimate(currentQuality, estimate.confidence(), true));
        } catch (SignalQualityException degenerate) {
            // A numerically degenerate window is not a session failure (08 §5); skip this estimate.
            transitionQuality(stateMachine.afterEstimate(currentQuality, 0.0, false));
        }
    }

    private void transitionQuality(SignalQuality next) {
        if (!next.equals(currentQuality)) {
            currentQuality = next;
            observer.onSignalQualityChanged(next);
        }
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, was " + value);
        }
        return value;
    }
}
