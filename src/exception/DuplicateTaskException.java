package exception;

/** Thrown when a task id already exists in the manager. */
public class DuplicateTaskException extends Exception {
    private static final long serialVersionUID = 1L;

    public DuplicateTaskException(String id) {
        super("Task " + id + " already exists.");
    }
}
