package id.asr.rppgvitals.domain.signal;

import java.time.Instant;
import java.util.Objects;

/// One sample of the extracted rPPG signal: a timestamp and the spatial-mean amplitude of each RGB
/// channel within the region of interest (`03_ARCHITECTURE.md §3`, `07_SIGNAL_PROCESSING.md §4`).
///
/// One `PpgSample` is produced per processed frame; an ordered window of them forms a [PpgWaveform].
///
/// @param timestamp the instant the underlying frame was captured; never `null`
/// @param red the spatial-mean amplitude of the red channel; a finite value
/// @param green the spatial-mean amplitude of the green channel; a finite value
/// @param blue the spatial-mean amplitude of the blue channel; a finite value
public record PpgSample(Instant timestamp, double red, double green, double blue) {

    /// Validates the timestamp and that every channel amplitude is finite.
    public PpgSample {
        Objects.requireNonNull(timestamp, "timestamp");
        requireFinite(red, "red");
        requireFinite(green, "green");
        requireFinite(blue, "blue");
    }

    private static void requireFinite(double value, String channel) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(channel + " amplitude must be finite, was " + value);
        }
    }
}
