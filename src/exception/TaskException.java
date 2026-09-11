package exception;

/**
 * Single checked exception for all domain-level errors.
 * Use {@link #getErrorCode()} to distinguish error types.
 */
public class TaskException extends Exception {
    private static final long serialVersionUID = 1L;
    private final ErrorCode errorCode;

    public TaskException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TaskException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** Categorises the kind of domain error. */
    public enum ErrorCode {
        TASK_NOT_FOUND,
        DUPLICATE_TASK,
        INVALID_TASK
    }
}