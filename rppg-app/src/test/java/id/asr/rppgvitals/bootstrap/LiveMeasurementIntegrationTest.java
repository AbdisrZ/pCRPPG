package id.asr.rppgvitals.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.application.usecase.measurement.LiveMeasurementOrchestrator;
import id.asr.rppgvitals.application.usecase.measurement.MeasurementObserver;
import id.asr.rppgvitals.domain.capture.CaptureConfiguration;
import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.capture.FrameSource;
import id.asr.rppgvitals.domain.detection.InferenceEngine;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;
import id.asr.rppgvitals.domain.estimation.ChromSignalEstimator;
import id.asr.rppgvitals.domain.estimation.HeartRateEstimate;
import id.asr.rppgvitals.domain.session.MeasurementSession;
import id.asr.rppgvitals.domain.signal.SignalQuality;

/// End-to-end integration of the whole live-measurement stack, driven by a synthetic frame source so
/// it runs with no camera, no ONNX model, and no display (`13_TESTING.md §2`). It exercises the real
/// path — `FrameSource` → `LiveMeasurementOrchestrator` (executors + queue) → `InferenceEngine` →
/// spatial averaging → `ChromSignalEstimator` — and asserts a known synthetic pulse is recovered as a
/// heart rate through the `MeasurementObserver` callback.
class LiveMeasurementIntegrationTest {

    private static final double PULSE_HZ = 1.2;
    private static final double EXPECTED_BPM = PULSE_HZ * 60.0;
    private static final int FRAME_RATE = 30;

    @Test
    void syntheticPulse_flowsThroughTheWholePipeline_toARecoveredHeartRate() throws InterruptedException {
        FrameSource frameSource = new SyntheticFrameSource(8, 8, PULSE_HZ, FRAME_RATE);
        InferenceEngine inferenceEngine = new WholeFrameInferenceEngine(8, 8);
        CapturingObserver observer = new CapturingObserver();
        MeasurementSession session = new MeasurementSession(UUID.randomUUID(), "synthetic", Instant.now());

        try (LiveMeasurementOrchestrator orchestrator =
                new LiveMeasurementOrchestrator(frameSource, inferenceEngine, new ChromSignalEstimator())) {
            orchestrator.start(session, observer, new CaptureConfiguration("synthetic", 8, 8, FRAME_RATE));
            assertTrue(observer.firstEstimate.await(10, TimeUnit.SECONDS), "expected an estimate");
            orchestrator.stop();
        }

        double bpm = observer.captured.get().beatsPerMinute();
        assertTrue(Math.abs(bpm - EXPECTED_BPM) < 6.0, "recovered " + bpm + " bpm, expected ~" + EXPECTED_BPM);
        assertTrue(session.estimates().size() >= 1, "session should have accumulated the estimate");
    }

    /// A frame source whose uniform pixels carry a sinusoidal pulse per channel (green strongest), so
    /// the downstream spatial mean is a clean synthetic rPPG signal at {@link #PULSE_HZ}.
    private static final class SyntheticFrameSource implements FrameSource {
        private final int width;
        private final int height;
        private final double pulseHz;
        private final int frameRate;
        private final AtomicLong sequence = new AtomicLong();

        SyntheticFrameSource(int width, int height, double pulseHz, int frameRate) {
            this.width = width;
            this.height = height;
            this.pulseHz = pulseHz;
            this.frameRate = frameRate;
        }

        @Override
        public List<String> availableDeviceIds() {
            return List.of("synthetic");
        }

        @Override
        public void open(CaptureConfiguration configuration) {
            // no device to open
        }

        @Override
        public Frame nextFrame() {
            long index = sequence.getAndIncrement();
            double pulse = Math.sin(2.0 * Math.PI * pulseHz * index / frameRate);
            byte red = (byte) Math.round(128 + 9 * pulse);
            byte green = (byte) Math.round(128 + 30 * pulse);
            byte blue = (byte) Math.round(128 + 3 * pulse);
            byte[] pixels = new byte[width * height * Frame.CHANNELS];
            for (int i = 0; i < width * height; i++) {
                pixels[i * Frame.CHANNELS] = red;
                pixels[i * Frame.CHANNELS + 1] = green;
                pixels[i * Frame.CHANNELS + 2] = blue;
            }
            // Model a real camera's cadence and yield this virtual thread.
            LockSupport.parkNanos(50_000L);
            return new Frame(pixels, width, height, index, Instant.now());
        }

        @Override
        public boolean isDeviceAvailable() {
            return true;
        }

        @Override
        public void close() {
            // nothing to release
        }
    }

    /// An inference engine that always reports the whole frame as the region of interest.
    private record WholeFrameInferenceEngine(int width, int height) implements InferenceEngine {
        @Override
        public Optional<RegionOfInterest> detectRegionOfInterest(Frame frame) {
            return Optional.of(new RegionOfInterest(0, 0, width, height, 0.95));
        }
    }

    private static final class CapturingObserver implements MeasurementObserver {
        private final CountDownLatch firstEstimate = new CountDownLatch(1);
        private final AtomicReference<HeartRateEstimate> captured = new AtomicReference<>();

        @Override
        public void onHeartRateUpdated(HeartRateEstimate estimate) {
            captured.set(estimate);
            firstEstimate.countDown();
        }

        @Override
        public void onSignalQualityChanged(SignalQuality quality) {
            // not asserted
        }

        @Override
        public void onSessionDegraded(String reason) {
            // not asserted
        }

        @Override
        public void onSessionRecovered() {
            // not asserted
        }
    }
}
