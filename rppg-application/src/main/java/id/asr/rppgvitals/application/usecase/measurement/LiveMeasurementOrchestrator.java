package id.asr.rppgvitals.application.usecase.measurement;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import id.asr.rppgvitals.domain.capture.CaptureConfiguration;
import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.capture.FrameSource;
import id.asr.rppgvitals.domain.detection.InferenceEngine;
import id.asr.rppgvitals.domain.estimation.SignalEstimator;
import id.asr.rppgvitals.domain.exception.CameraUnavailableException;
import id.asr.rppgvitals.domain.session.MeasurementSession;

/// The live measurement pipeline as a sustained, multi-threaded process (`03_ARCHITECTURE.md §6.2`,
/// `11_THREADING.md`). Between [#start] and [#stop] it runs two long-running virtual-thread tasks — a
/// capture loop pulling frames from [FrameSource], and a processing loop driving the
/// [MeasurementPipeline] — handing frames across a bounded, drop-oldest queue (`11 §2`, `§4`).
///
/// **Confinement.** The [MeasurementPipeline] is touched only by the processing task (`11 §5`). A
/// camera loss detected on the capture thread is published through a `volatile` reason; the processing
/// thread reads it and dispatches the degraded/recovered transition to the pipeline, so no cross-thread
/// call into the pipeline ever occurs.
///
/// **Thread-safety.** [#start], [#stop], and [#close] are driven by a single controlling caller (the
/// measurement use cases / composition root), not concurrently; the concurrency this type manages is
/// its own two internal tasks. Executors are shut down deterministically on [#close] (`11 §8`).
public final class LiveMeasurementOrchestrator implements AutoCloseable {

    /// The session correlation identifier, bound per task at each loop's start (`11_THREADING.md §6`,
    /// `05_CODING_STANDARD.md §6`).
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    private static final int QUEUE_DEPTH = 3;
    private static final double ANALYSIS_WINDOW_SECONDS = 8.0;
    private static final double ESTIMATE_INTERVAL_SECONDS = 1.0;
    private static final long RECONNECT_POLL_NANOS = 200_000_000L;
    private static final long PROCESSING_POLL_MILLIS = 100L;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

    private final FrameSource frameSource;
    private final InferenceEngine inferenceEngine;
    private final SignalEstimator estimator;
    private final ExecutorService captureExecutor;
    private final ExecutorService processingExecutor;

    private volatile boolean running;
    private volatile String degradationReason;
    private BlockingQueue<Frame> frameQueue;
    private MeasurementPipeline pipeline;
    private Future<?> captureTask;
    private Future<?> processingTask;

    /// Creates the orchestrator with its two dedicated virtual-thread executors (`11 §2`).
    ///
    /// @param frameSource the capture port; never `null`
    /// @param inferenceEngine the face-detection port; never `null`
    /// @param estimator the heart-rate estimator; never `null`
    public LiveMeasurementOrchestrator(
            FrameSource frameSource, InferenceEngine inferenceEngine, SignalEstimator estimator) {
        this.frameSource = Objects.requireNonNull(frameSource, "frameSource");
        this.inferenceEngine = Objects.requireNonNull(inferenceEngine, "inferenceEngine");
        this.estimator = Objects.requireNonNull(estimator, "estimator");
        this.captureExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.processingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /// Opens the device and starts the capture and processing loops for a session.
    ///
    /// @param session the session whose estimates the run accumulates; never `null`
    /// @param observer the callback sink for live results; never `null`
    /// @param configuration the device and capture parameters; never `null`
    /// @throws IllegalStateException if a session is already running
    /// @throws CameraUnavailableException if the device cannot be opened
    public void start(MeasurementSession session, MeasurementObserver observer, CaptureConfiguration configuration) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(configuration, "configuration");
        if (running) {
            throw new IllegalStateException("a session is already running");
        }
        frameSource.open(configuration);
        int framesPerSecond = configuration.targetFrameRate();
        int windowSamples = (int) Math.round(ANALYSIS_WINDOW_SECONDS * framesPerSecond);
        int intervalFrames = Math.max(1, (int) Math.round(ESTIMATE_INTERVAL_SECONDS * framesPerSecond));
        this.pipeline = new MeasurementPipeline(
                inferenceEngine, estimator, observer, session, windowSamples, intervalFrames, framesPerSecond);
        this.frameQueue = new ArrayBlockingQueue<>(QUEUE_DEPTH);
        this.degradationReason = null;
        this.running = true;
        String correlationId = session.id().toString();
        this.captureTask = captureExecutor.submit(
                () -> ScopedValue.where(CORRELATION_ID, correlationId).run(this::runCaptureLoop));
        this.processingTask = processingExecutor.submit(
                () -> ScopedValue.where(CORRELATION_ID, correlationId).run(this::runProcessingLoop));
    }

    /// Cooperatively stops the current session: the capture loop is interrupted, the processing loop
    /// drains the queue and exits, and the device is released (`11 §7`). Idempotent and no-op if not
    /// running.
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        captureTask.cancel(true);
        awaitQuietly(processingTask);
        awaitQuietly(captureTask);
        frameSource.close();
    }

    /// Stops any running session and shuts down both executors deterministically (`11 §8`).
    @Override
    public void close() {
        stop();
        shutdown(captureExecutor);
        shutdown(processingExecutor);
    }

    private void runCaptureLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Frame frame = frameSource.nextFrame();
                degradationReason = null;
                offerDropOldest(frame);
            } catch (CameraUnavailableException unavailable) {
                degradationReason = unavailable.getMessage();
                if (!awaitReconnect()) {
                    return;
                }
                degradationReason = null;
            }
        }
    }

    private void offerDropOldest(Frame frame) {
        // Single producer: if the queue is full, discard the oldest frame and retry — a live display
        // should show the most recent frame, not a backlog (11 §4). Terminates in at most two tries.
        while (!frameQueue.offer(frame)) {
            frameQueue.poll();
        }
    }

    private boolean awaitReconnect() {
        while (running && !Thread.currentThread().isInterrupted()) {
            if (frameSource.isDeviceAvailable()) {
                return true;
            }
            LockSupport.parkNanos(RECONNECT_POLL_NANOS);
        }
        return false;
    }

    private void runProcessingLoop() {
        String reflected = null;
        while (running || !frameQueue.isEmpty()) {
            reflected = reflectDegradation(reflected);
            try {
                Frame frame = frameQueue.poll(PROCESSING_POLL_MILLIS, TimeUnit.MILLISECONDS);
                if (frame != null) {
                    pipeline.onFrame(frame);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private String reflectDegradation(String reflected) {
        String current = degradationReason;
        if (current != null && reflected == null) {
            pipeline.onDegraded(current);
            return current;
        }
        if (current == null && reflected != null) {
            pipeline.onRecovered();
            return null;
        }
        return reflected;
    }

    private static void awaitQuietly(Future<?> task) {
        try {
            task.get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (CancellationException cancelled) {
            // Expected: the capture task is cancelled by stop() before it is awaited here.
        } catch (ExecutionException | TimeoutException failed) {
            task.cancel(true);
        }
    }

    private static void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
