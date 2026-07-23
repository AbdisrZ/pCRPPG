package id.asr.rppgvitals.domain.exception;

/// Signals that application configuration is missing or invalid (`00_MASTER_PROMPT.md §22.2`).
///
/// Carries the configuration key involved so the failure points the operator at what to fix.
public final class ConfigurationException extends RppgApplicationException {

    private static final long serialVersionUID = 1L;

    private final String configKey;

    /// Creates the exception for a specific configuration key.
    ///
    /// @param configKey the configuration key that is missing or invalid; never `null`
    /// @param message the actionable failure description; never `null`
    public ConfigurationException(String configKey, String message) {
        super(message);
        this.configKey = configKey;
    }

    /// Creates the exception for a specific configuration key, wrapping an underlying cause.
    ///
    /// @param configKey the configuration key that is missing or invalid; never `null`
    /// @param message the actionable failure description; never `null`
    /// @param cause the underlying failure being translated; may be `null`
    public ConfigurationException(String configKey, String message, Throwable cause) {
        super(message, cause);
        this.configKey = configKey;
    }

    /// Returns the configuration key that is missing or invalid.
    ///
    /// @return the configuration key
    public String configKey() {
        return configKey;
    }
}
