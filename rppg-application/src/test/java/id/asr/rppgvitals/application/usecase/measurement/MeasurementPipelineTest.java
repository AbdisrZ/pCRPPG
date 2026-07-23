package id.asr.rppgvitals.application.usecase.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.InferenceEngine;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;
import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;
import id.asr.rppgvitals.domain.estimation.SignalEstimator;
import id.asr.rppgvitals.domain.exception.SignalQualityException;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.signal.SignalQuality;

class MeasurementPipelineTest {

    private static final Instant NOW = Instant.parse("2026-07-23T09:00:00Z");
    private static final RegionOfInterest ROI = new RegionOfInterest(0, 0, 2, 2, 0.9);

    private final InferenceEngine inferenceEngine = mock(InferenceEngine.class);
    private final SignalEstimator estimator = mock(SignalEstimator.class);
    private final RecordingObserver observer = new RecordingObserver();
    private final MeasurementSession session = new MeasurementSession(UUID.randomUUID(), "cam-0", NOW);

    private MeasurementPipeline pipeline() {
        return new MeasurementPipeline(inferenceEngine, estimator, observer, session, 3, 1, 30.0);
    }

    private static Frame frame() {
        return new Frame(new byte[2 * 2 * Frame.CHANNELS], 2, 2, 0L, NOW);
    }

    private void feed(int frames) {
        MeasurementPipeline pipeline = pipeline();
        for (int i = 0; i < frames; i++) {
            pipeline.onFrame(frame());
        }
    }

    @Test
    void onFrame_whenWindowFills_estimatesAndReportsStable() {
        when(inferenceEngine.detectRegionOfInterest(any())).thenReturn(Optional.of(ROI));
        when(estimator.estimate(any())).thenReturn(new HeartRateEstimate(72.0, 0.9, NOW, "CHROM"));

        feed(3);

        assertEquals(1, observer.estimates.size());
        assertEquals(72.0, observer.estimates.get(0).beatsPerMinute());
        assertEquals(1, session.estimates().size());
        assertInstanceOf(SignalQuality.Stable.class, observer.qualityChanges.get(observer.qualityChanges.size() - 1));
    }

    @Test
    void onFrame_beforeWindowFills_producesNoEstimate() {
        when(inferenceEngine.detectRegionOfInterest(any())).thenReturn(Optional.of(ROI));

        feed(2);

        assertTrue(observer.estimates.isEmpty());
        assertTrue(session.estimates().isEmpty());
    }

    @Test
    void onFrame_withNoFace_addsNoSampleAndNeverEstimates() {
        when(inferenceEngine.detectRegionOfInterest(any())).thenReturn(Optional.empty());

        feed(10);

        assertTrue(observer.estimates.isEmpty());
    }

    @Test
    void onFrame_whenEstimatorReportsDegenerateWindow_skipsTheEstimate() {
        when(inferenceEngine.detectRegionOfInterest(any())).thenReturn(Optional.of(ROI));
        when(estimator.estimate(any())).thenThrow(new SignalQualityException("CHROM", "degenerate"));

        feed(3);

        assertTrue(observer.estimates.isEmpty());
        assertTrue(session.estimates().isEmpty());
    }

    @Test
    void onDegraded_notifiesTheObserver() {
        pipeline().onDegraded("camera disconnected");

        assertEquals(List.of("camera disconnected"), observer.degradedReasons);
    }

    @Test
    void onRecovered_notifiesTheObserver() {
        MeasurementPipeline pipeline = pipeline();
        pipeline.onDegraded("camera disconnected");

        pipeline.onRecovered();

        assertEquals(1, observer.recoveredCount);
    }

    @Test
    void constructor_withInvalidConfiguration_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new MeasurementPipeline(null, estimator, observer, session, 3, 1, 30.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MeasurementPipeline(inferenceEngine, estimator, observer, session, 0, 1, 30.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MeasurementPipeline(inferenceEngine, estimator, observer, session, 3, 1, 0.0));
    }

    private static final class RecordingObserver implements MeasurementObserver {
        private final List<HeartRateEstimate> estimates = new ArrayList<>();
        private final List<SignalQuality> qualityChanges = new ArrayList<>();
        private final List<String> degradedReasons = new ArrayList<>();
        private int recoveredCount;

        @Override
        public void onHeartRateUpdated(HeartRateEstimate estimate) {
            estimates.add(estimate);
        }

        @Override
        public void onSignalQualityChanged(SignalQuality quality) {
            qualityChanges.add(quality);
        }

        @Override
        public void onSessionDegraded(String reason) {
            degradedReasons.add(reason);
        }

        @Override
        public void onSessionRecovered() {
            recoveredCount++;
        }
    }
}
