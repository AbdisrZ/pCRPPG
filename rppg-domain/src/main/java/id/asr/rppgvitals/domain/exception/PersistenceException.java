package id.asr.rppgvitals.domain.exception;

/// Signals that a persistence operation failed (`00_MASTER_PROMPT.md §22.2`).
///
/// Translated from JDBC `SQLException`s at the persistence adapter boundary (`00 §22.1`) so the
/// domain and application layers never see a checked `SQLException`. Carries the name of the
/// operation that failed for context.
public final class PersistenceException extends RppgApplicationException {

    private static final long serialVersionUID = 1L;

    private final String operation;

    /// Creates the exception for a specific operation.
    ///
    /// @param operation the name of the persistence operation that failed (for example `save` or
    ///     `deleteById`); never `null`
    /// @param message the actionable failure description; never `null`
    public PersistenceException(String operation, String message) {
        super(message);
        this.operation = operation;
    }

    /// Creates the exception for a specific operation, wrapping an underlying cause.
    ///
    /// @param operation the name of the persistence operation that failed; never `null`
    /// @param message the actionable failure description; never `null`
    /// @param cause the underlying JDBC failure being translated; may be `null`
    public PersistenceException(String operation, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
    }

    /// Returns the name of the persistence operation that failed.
    ///
    /// @return the operation name
    public String operation() {
        return operation;
    }
}
