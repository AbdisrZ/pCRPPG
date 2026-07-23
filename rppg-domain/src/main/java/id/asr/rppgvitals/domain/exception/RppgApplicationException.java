package id.asr.rppgvitals.domain.exception;

/// The sealed root of the project's exception hierarchy (`00_MASTER_PROMPT.md §22.2`).
///
/// Being sealed lets the presentation boundary handle every failure mode exhaustively with a
/// pattern-matched `switch`. All subtypes are unchecked (they extend [RuntimeException]): operational
/// failures from infrastructure libraries — `SQLException`, OpenCV and ONNX Runtime error types — are
/// translated into one of these subtypes at the adapter boundary (`00 §22.1`) and never leak inward
/// as their original checked or vendor types. Every subtype carries actionable context; a bare
/// `"an error occurred"` message is forbidden (`00 §22.2`).
public abstract sealed class RppgApplicationException extends RuntimeException
        permits CameraUnavailableException,
                SignalQualityException,
                ModelInferenceException,
                PersistenceException,
                ConfigurationException {

    private static final long serialVersionUID = 1L;

    /// Creates an exception with an actionable message.
    ///
    /// @param message the failure description, including enough context to act on; never `null`
    protected RppgApplicationException(String message) {
        super(message);
    }

    /// Creates an exception with an actionable message and an underlying cause.
    ///
    /// @param message the failure description, including enough context to act on; never `null`
    /// @param cause the underlying failure being translated at an adapter boundary; may be `null`
    protected RppgApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
