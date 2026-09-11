package model;

import exception.TaskException;

/**
 * Task priority. {@link #MEDIUM} is the default for new tasks.
 */
public enum Priority {
    LOW, MEDIUM, HIGH;

    /**
     * Parses user input. {@code null} or blank returns {@link #MEDIUM}.
     * Unknown non-blank input raises {@link TaskException}.
     */
    public static Priority fromString(String raw) throws TaskException {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        String trimmed = raw.trim().toUpperCase();
        for (Priority p : values()) {
            if (p.name().equals(trimmed)) {
                return p;
            }
        }
        throw new TaskException(TaskException.ErrorCode.INVALID_TASK,
                "Unknown priority: '" + raw + "'. Use LOW, MEDIUM, or HIGH.");
    }
}
