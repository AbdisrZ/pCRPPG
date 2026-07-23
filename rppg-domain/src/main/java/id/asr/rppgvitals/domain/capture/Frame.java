package id.asr.rppgvitals.domain.capture;

import java.time.Instant;
import java.util.Objects;

/// A single captured camera frame as it enters the domain: its pixel buffer plus the metadata needed
/// to place it in time and order (`03_ARCHITECTURE.md §3`).
///
/// **Canonical pixel layout.** Pixels are 8-bit **RGB**, three [#CHANNELS] per pixel, interleaved and
/// row-major (`pixels[(y * width + x) * 3 + c]`), so `pixels.length == width * height * 3`. This is
/// the domain's canonical format; the capture adapter is responsible for converting its native layout
/// (for example OpenCV's BGR) into this one when it constructs a `Frame`, so that domain-side spatial
/// averaging (`07_SIGNAL_PROCESSING.md §4`) can interpret the buffer without knowing the backend. See
/// ADR 0007.
///
/// The buffer is defensively copied on construction and on access, so a `Frame` is a genuinely
/// immutable value object (`05_CODING_STANDARD.md §6`).
///
/// **Performance note.** The per-frame copy is the correctness-first default (`00_MASTER_PROMPT.md §5`);
/// whether it is a bottleneck against the frame budget of `00 §11` is a question for measurement, not
/// assumption (`00 §32`). Any zero-copy optimisation is deferred to the latency work of Phase 5 and,
/// if adopted, is introduced behind this same type with a recorded decision.
///
/// @param pixels the RGB pixel buffer in the canonical layout above; never `null`
/// @param width the frame width in pixels; strictly positive
/// @param height the frame height in pixels; strictly positive
/// @param sequenceIndex the monotonically increasing capture index within a session; never negative
/// @param capturedAt the instant the frame was captured; never `null`
public record Frame(byte[] pixels, int width, int height, long sequenceIndex, Instant capturedAt) {

    /// The number of colour channels per pixel in the canonical RGB layout.
    public static final int CHANNELS = 3;

    /// Validates frame invariants and defensively copies the pixel buffer.
    public Frame {
        Objects.requireNonNull(pixels, "pixels");
        Objects.requireNonNull(capturedAt, "capturedAt");
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive, was " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive, was " + height);
        }
        if (sequenceIndex < 0) {
            throw new IllegalArgumentException("sequenceIndex must not be negative, was " + sequenceIndex);
        }
        int expectedLength = width * height * CHANNELS;
        if (pixels.length != expectedLength) {
            throw new IllegalArgumentException(
                    "pixels length must be width*height*3 = " + expectedLength + ", was " + pixels.length);
        }
        pixels = pixels.clone();
    }

    /// Returns a defensive copy of the pixel buffer, preserving immutability.
    ///
    /// @return a fresh copy of the pixel data
    @Override
    public byte[] pixels() {
        return pixels.clone();
    }

    /// Returns the number of pixels implied by the frame dimensions.
    ///
    /// @return the product of width and height
    public long pixelCount() {
        return (long) width * height;
    }
}
