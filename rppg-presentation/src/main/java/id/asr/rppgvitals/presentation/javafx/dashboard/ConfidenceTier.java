package id.asr.rppgvitals.presentation.javafx.dashboard;

/// The three display tiers for the confidence indicator (`06_UI_GUIDELINE.md §3`), distinct from the
/// raw continuous confidence score a `HeartRateEstimate` carries. A tier applies only once an estimate
/// actually exists (signal quality `STABLE`); while searching or degraded, no tier is shown.
///
/// Each tier pairs a colour with a plain-language guidance message — never a bare severity word
/// (`06 §3`, Design Principle 2) — so the state is conveyed redundantly by colour, icon, and text
/// (`06 §9`). Shared by the live and history screens so the tiering logic is defined once (`06 §6.3`).
public enum ConfidenceTier {

    /// High confidence: a stable, trustworthy reading (score ≥ 0.8).
    HIGH("#2E7D32", "Reading stable"),

    /// Moderate confidence: a usable but variable reading (score 0.5–0.79).
    MODERATE("#F9A825", "Reading may vary — try to stay still"),

    /// Low confidence: a computed value that should be treated with caution (score < 0.5).
    LOW("#D84315", "Low confidence — check lighting and position");

    private static final double HIGH_THRESHOLD = 0.8;
    private static final double MODERATE_THRESHOLD = 0.5;

    private final String colorHex;
    private final String message;

    ConfidenceTier(String colorHex, String message) {
        this.colorHex = colorHex;
        this.message = message;
    }

    /// Returns the tier's indicative colour as a CSS hex string (`06 §7`).
    ///
    /// @return the hex colour, for example `#2E7D32`
    public String colorHex() {
        return colorHex;
    }

    /// Returns the tier's guidance message shown alongside the reading (`06 §3`).
    ///
    /// @return the plain-language guidance text
    public String message() {
        return message;
    }

    /// Maps an estimate's confidence score to its display tier (`06 §3`).
    ///
    /// @param confidence the estimate confidence in the closed range `[0, 1]`
    /// @return the corresponding display tier
    public static ConfidenceTier fromConfidence(double confidence) {
        if (confidence >= HIGH_THRESHOLD) {
            return HIGH;
        }
        if (confidence >= MODERATE_THRESHOLD) {
            return MODERATE;
        }
        return LOW;
    }
}
