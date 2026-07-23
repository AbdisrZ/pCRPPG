package id.asr.rppgvitals.domain.exception;

/// Signals a genuinely anomalous failure in the estimation math (`00_MASTER_PROMPT.md §22.2`,
/// `08_ESTIMATOR_ENGINE.md §5`).
///
/// This is *not* thrown for the routine states of low signal quality, no face, or "still searching"
/// — those are represented as data (a low-confidence estimate or a
/// [id.asr.rppgvitals.domain.signal.SignalQuality] state), never as an exception (`00 §22.2`). It is
/// reserved for numerically degenerate input, such as a zero-variance window that would divide by
/// zero in a CHROM/POS weighting step. Carries the identifier of the algorithm that failed.
public final class SignalQualityException extends RppgApplicationException {

    private static final long serialVersionUID = 1L;

    private final String algorithm;

    /// Creates the exception for a specific estimator.
    ///
    /// @param algorithm the identifier of the estimation algorithm that hit the degeneracy; never `null`
    /// @param message the actionable failure description; never `null`
    public SignalQualityException(String algorithm, String message) {
        super(message);
        this.algorithm = algorithm;
    }

    /// Creates the exception for a specific estimator, wrapping an underlying cause.
    ///
    /// @param algorithm the identifier of the estimation algorithm that hit the degeneracy; never `null`
    /// @param message the actionable failure description; never `null`
    /// @param cause the underlying arithmetic failure being translated; may be `null`
    public SignalQualityException(String algorithm, String message, Throwable cause) {
        super(message, cause);
        this.algorithm = algorithm;
    }

    /// Returns the identifier of the estimation algorithm that hit the degeneracy.
    ///
    /// @return the algorithm identifier
    public String algorithm() {
        return algorithm;
    }
}
