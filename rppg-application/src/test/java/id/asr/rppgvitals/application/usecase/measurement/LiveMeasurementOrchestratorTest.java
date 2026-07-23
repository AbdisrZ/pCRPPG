package id.asr.rppgvitals.application.usecase.measurement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.capture.CaptureConfiguration;
import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.capture.FrameSource;
import id.asr.rppgvitals.domain.detection.InferenceEngine;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;
import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;
import id.asr.rppgvitals.domain.estimation.SignalEstimator;
import id.asr.rppgvitals.domain.exception.CameraUnavailableException;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.signal.SignalQuality;

class LiveMeasurementOrchestratorTest {

    private static final Instant NOW = Instant.parse("2026-07-23T09:00:00Z");
    private static final RegionOfInterest ROI = new RegionOfInterest(0, 0, 2, 2, 0.9);
    private static final CaptureConfiguration CONFIG = new CaptureConfiguration("cam-0", 2, 2, 30);

    private final InferenceEngine inferenceEngine = mock(InferenceEngine.class);
    private final SignalEstimator estimator = mock(SignalEstimator.class);
    private final LatchObserver observer = new LatchObserver();
    private final MeasurementSession session = new MeasurementSession(UUID.randomUUID(), "cam-0", NOW);

    @Test
    void run_producesEstimatesAndClosesTheDeviceOnStop() throws InterruptedException {
        when(inferenceEngine.detectRegionOfInterest(any())).thenReturn(Optional.of(ROI));
        when(estimator.estimate(any())).thenReturn(new HeartRateEstimate(72.0, 0.9, NOW, "CHROM"));
        FakeFrameSource frameSource = new FakeFrameSource();

        try (LiveMeasurementOrchestrator orchestrator =
                new LiveMeasurementOrchestrator(frameSource, inferenceEngine, estimator)) {
            orchestrator.start(session, observer, CONFIG);
            assertTrue(observer.estimate.await(5, TimeUnit.SECONDS), "expected at least one estimate");
            orchestrator.stop();
        }

        assertFalse(session.estimates().isEmpty());
        assertTrue(frameSource.closed);
    }

    @Test
    void start_whileAlreadyRunning_throws() {
        when(inferenceEngine.detectRegionOfInterest(any())).thenReturn(Optional.empty());
        FakeFrameSource frameSource = new FakeFrameSource();

        try (LiveMeasurementOrchestrator orchestrator =
                new LiveMeasurementOrchestrator(frameSource, inferenceEngine, estimator)) {
            orchestrator.start(session, observer, CONFIG);

            assertThrows(IllegalStateException.class, () -> orchestrator.start(session, observer, CONFIG));

            orchestrator.stop();
        }
    }

    @Test
    void cameraLoss_thenRecovery_notifiesTheObserver() throws InterruptedException {
        when(inferenceEngine.detectRegionOfInterest(any())).thenReturn(Optional.empty());
        FakeFrameSource frameSource = new FakeFrameSource();
        frameSource.healthy = false;

        try (LiveMeasurementOrchestrator orchestrator =
                new LiveMeasurementOrchestrator(frameSource, inferenceEngine, estimator)) {
            orchestrator.start(session, observer, CONFIG);
            assertTrue(observer.degraded.await(5, TimeUnit.SECONDS), "expected a degraded notification");

            frameSource.healthy = true;
            assertTrue(observer.recovered.await(5, TimeUnit.SECONDS), "expected a recovered notification");

            orchestrator.stop();
        }
    }

    /// A frame source that yields frames instantly while healthy and throws while not, so the loops can
    /// be driven deterministically without timing assumptions.
    private static final class FakeFrameSource implements FrameSource {
        private final AtomicLong sequence = new AtomicLong();
        private volatile boolean healthy = true;
        private volatile boolean closed;

        @Override
        public List<String> availableDeviceIds() {
            return List.of("cam-0");
        }

        @Override
        public void open(CaptureConfiguration configuration) {
            // no-op for the fake
        }

        @Override
        public Frame nextFrame() {
            if (!healthy) {
                throw new CameraUnavailableException("cam-0", "camera lost");
            }
            // Model a real camera's frame cadence, which also yields this virtual thread so the tight
            // capture loop cannot pin its carrier during the test.
            LockSupport.parkNanos(100_000L);
            return new Frame(new byte[2 * 2 * Frame.CHANNELS], 2, 2, sequence.getAndIncrement(), NOW);
        }

        @Override
        public boolean isDeviceAvailable() {
            return healthy;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class LatchObserver implements MeasurementObserver {
        private final CountDownLatch estimate = new CountDownLatch(1);
        private final CountDownLatch degraded = new CountDownLatch(1);
        private final CountDownLatch recovered = new CountDownLatch(1);

        @Override
        public void onHeartRateUpdated(HeartRateEstimate heartRateEstimate) {
            estimate.countDown();
        }

        @Override
        public void onSignalQualityChanged(SignalQuality quality) {
            // not asserted here
        }

        @Override
        public void onSessionDegraded(String reason) {
            degraded.countDown();
        }

        @Override
        public void onSessionRecovered() {
            recovered.countDown();
        }
    }
}
