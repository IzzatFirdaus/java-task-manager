package exception;

/** Thrown when an id lookup against {@link service.TaskManager} fails. */
public class TaskNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public TaskNotFoundException(String id) {
        super("Task " + id + " not found.");
    }
}
