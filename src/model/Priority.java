package model;

import exception.InvalidTaskException;

/**
 * Task priority. {@link #MEDIUM} is the default for new tasks.
 */
public enum Priority {
    LOW, MEDIUM, HIGH;

    /**
     * Parses user input. {@code null} or blank returns {@link #MEDIUM}.
     * Unknown non-blank input raises {@link InvalidTaskException}.
     */
    public static Priority fromString(String raw) {
        // STEP_7_IMPLEMENT: implement canonical parser.
        throw new UnsupportedOperationException("Priority.fromString not yet implemented");
    }
}
