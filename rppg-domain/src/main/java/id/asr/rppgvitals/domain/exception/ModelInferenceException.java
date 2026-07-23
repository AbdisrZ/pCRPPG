package id.asr.rppgvitals.domain.exception;

/// Signals that the face/landmark inference model failed to run (`00_MASTER_PROMPT.md §22.2`).
///
/// Translated from ONNX Runtime error types at the inference adapter boundary (`00 §22.1`). Note the
/// normal absence of a face is *not* an error — it is a typed empty result (`00 §22.2`); this
/// exception is for genuine model-execution failures. Carries the model identifier for context.
public final class ModelInferenceException extends RppgApplicationException {

    private static final long serialVersionUID = 1L;

    private final String modelIdentifier;

    /// Creates the exception for a specific model.
    ///
    /// @param modelIdentifier the identifier of the model that failed to run; never `null`
    /// @param message the actionable failure description; never `null`
    public ModelInferenceException(String modelIdentifier, String message) {
        super(message);
        this.modelIdentifier = modelIdentifier;
    }

    /// Creates the exception for a specific model, wrapping an underlying cause.
    ///
    /// @param modelIdentifier the identifier of the model that failed to run; never `null`
    /// @param message the actionable failure description; never `null`
    /// @param cause the underlying inference-runtime failure being translated; may be `null`
    public ModelInferenceException(String modelIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.modelIdentifier = modelIdentifier;
    }

    /// Returns the identifier of the model that failed to run.
    ///
    /// @return the model identifier
    public String modelIdentifier() {
        return modelIdentifier;
    }
}
