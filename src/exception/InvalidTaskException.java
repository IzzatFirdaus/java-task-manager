package exception;

/** Thrown when a {@link model.Task} field violates a domain rule. */
public class InvalidTaskException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidTaskException(String message) {
        super(message);
    }
}
