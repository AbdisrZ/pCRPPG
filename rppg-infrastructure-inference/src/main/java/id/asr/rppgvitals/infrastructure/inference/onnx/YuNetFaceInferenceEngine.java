package id.asr.rppgvitals.infrastructure.inference.onnx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

import nu.pattern.OpenCV;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;

import id.asr.rppgvitals.domain.capture.Frame;
import id.asr.rppgvitals.domain.detection.InferenceEngine;
import id.asr.rppgvitals.domain.detection.RegionOfInterest;
import id.asr.rppgvitals.domain.exception.ConfigurationException;

/// A real face-detection [InferenceEngine] backed by OpenCV's YuNet detector (`09_AI_INTEGRATION.md`,
/// ADR 0012).
///
/// It runs the bundled `face_detection_yunet_2023mar.onnx` model through OpenCV's DNN-based
/// `FaceDetectorYN`, returning the highest-scoring face in each frame as the [RegionOfInterest]. The
/// detector's per-face score becomes [RegionOfInterest#detectionConfidence], which drives both the
/// preview's probability label (`06 §6.2`) and the ROI-stability term of the heart-rate confidence
/// (`08 §3`).
///
/// **Thread-safety.** `FaceDetectorYN` is stateful (input size) and is confined to the single processing
/// task that owns this engine (`11 §5`); it is not safe for concurrent use.
public final class YuNetFaceInferenceEngine implements InferenceEngine {

    private static final String MODEL_RESOURCE = "/models/face_detection_yunet_2023mar.onnx";
    private static final float SCORE_THRESHOLD = 0.7f;
    private static final float NMS_THRESHOLD = 0.3f;
    private static final int TOP_K = 50;
    private static final int FACE_SCORE_COLUMN = 14;

    static {
        OpenCV.loadLocally();
    }

    private final FaceDetectorYN detector;
    private int inputWidth = -1;
    private int inputHeight = -1;

    /// Creates the engine, extracting the bundled model and initialising the detector.
    public YuNetFaceInferenceEngine() {
        Path model = extractModel();
        this.detector =
                FaceDetectorYN.create(model.toString(), "", new Size(320, 320), SCORE_THRESHOLD, NMS_THRESHOLD, TOP_K);
    }

    /// {@inheritDoc}
    @Override
    public Optional<RegionOfInterest> detectRegionOfInterest(Frame frame) {
        Objects.requireNonNull(frame, "frame");
        Mat bgr = toBgrMat(frame);
        Mat faces = new Mat();
        try {
            ensureInputSize(frame.width(), frame.height());
            detector.detect(bgr, faces);
            return bestFace(faces, frame.width(), frame.height());
        } finally {
            faces.release();
            bgr.release();
        }
    }

    private void ensureInputSize(int width, int height) {
        if (width != inputWidth || height != inputHeight) {
            detector.setInputSize(new Size(width, height));
            inputWidth = width;
            inputHeight = height;
        }
    }

    private static Optional<RegionOfInterest> bestFace(Mat faces, int frameWidth, int frameHeight) {
        if (faces.rows() == 0) {
            return Optional.empty();
        }
        int bestRow = 0;
        float bestScore = (float) faces.get(0, FACE_SCORE_COLUMN)[0];
        for (int row = 1; row < faces.rows(); row++) {
            float score = (float) faces.get(row, FACE_SCORE_COLUMN)[0];
            if (score > bestScore) {
                bestScore = score;
                bestRow = row;
            }
        }
        double confidence = Math.max(0.0, Math.min(1.0, bestScore));
        return Optional.of(foreheadRegion(faces, bestRow, frameWidth, frameHeight, confidence));
    }

    /// Derives the forehead ROI from the detected eye landmarks (`09_AI_INTEGRATION.md §6`): the
    /// forehead is the strongest, most motion-stable rPPG region (`07 §4`). Falls back to the upper
    /// third of the face box when the landmarks are too close together to be trustworthy.
    private static RegionOfInterest foreheadRegion(
            Mat faces, int row, int frameWidth, int frameHeight, double confidence) {
        double rightEyeX = faces.get(row, 4)[0];
        double rightEyeY = faces.get(row, 5)[0];
        double leftEyeX = faces.get(row, 6)[0];
        double leftEyeY = faces.get(row, 7)[0];
        double eyeSpan = Math.hypot(leftEyeX - rightEyeX, leftEyeY - rightEyeY);

        int faceX = (int) Math.round(faces.get(row, 0)[0]);
        int faceY = (int) Math.round(faces.get(row, 1)[0]);
        int faceW = (int) Math.round(faces.get(row, 2)[0]);
        int faceH = (int) Math.round(faces.get(row, 3)[0]);

        double boxX;
        double boxY;
        double boxW;
        double boxH;
        if (eyeSpan >= 12.0) {
            double eyeCentreX = (rightEyeX + leftEyeX) / 2.0;
            double eyeCentreY = (rightEyeY + leftEyeY) / 2.0;
            boxW = eyeSpan * 1.1;
            boxH = eyeSpan * 0.6;
            boxX = eyeCentreX - boxW / 2.0;
            boxY = eyeCentreY - eyeSpan * 0.9 - boxH / 2.0;
        } else {
            boxW = faceW * 0.6;
            boxH = faceH * 0.22;
            boxX = faceX + (faceW - boxW) / 2.0;
            boxY = faceY + faceH * 0.12;
        }

        int x = clamp((int) Math.round(boxX), 0, frameWidth - 1);
        int y = clamp((int) Math.round(boxY), 0, frameHeight - 1);
        int width = clamp((int) Math.round(boxW), 1, frameWidth - x);
        int height = clamp((int) Math.round(boxH), 1, frameHeight - y);
        return new RegionOfInterest(x, y, width, height, confidence);
    }

    private static Mat toBgrMat(Frame frame) {
        Mat rgb = new Mat(frame.height(), frame.width(), CvType.CV_8UC3);
        rgb.put(0, 0, frame.pixels());
        Mat bgr = new Mat();
        Imgproc.cvtColor(rgb, bgr, Imgproc.COLOR_RGB2BGR);
        rgb.release();
        return bgr;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Path extractModel() {
        try (InputStream in = YuNetFaceInferenceEngine.class.getResourceAsStream(MODEL_RESOURCE)) {
            if (in == null) {
                throw new ConfigurationException("face-model", "bundled face model not found: " + MODEL_RESOURCE);
            }
            Path model = Files.createTempFile("yunet-face-", ".onnx");
            model.toFile().deleteOnExit();
            Files.copy(in, model, StandardCopyOption.REPLACE_EXISTING);
            return model;
        } catch (IOException cause) {
            throw new ConfigurationException("face-model", "cannot stage the bundled face model", cause);
        }
    }
}
