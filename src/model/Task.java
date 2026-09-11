package model;

import exception.TaskException;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity. {@link #id} and {@link #createdAt} are immutable; the rest
 * evolve through service-layer mutators so the manager can enforce rules.
 */
public class Task {
    private final String id;
    private String title;
    private String description;
    private Priority priority;
    private boolean completed;
    private final Instant createdAt;

    /**
     * Creates a new task (sets {@link #createdAt} to now).
     */
    public Task(String id, String title, String description, Priority priority) throws TaskException {
        this(id, title, description, priority, Instant.now());
    }

    /**
     * Full constructor that accepts an explicit {@code createdAt}.
     * Used by the persistence layer when restoring tasks from disk.
     */
    public Task(String id, String title, String description, Priority priority, Instant createdAt) throws TaskException {
        if (id == null || id.isBlank()) throw new TaskException(TaskException.ErrorCode.INVALID_TASK, "id must not be blank.");
        if (title == null || title.isBlank()) throw new TaskException(TaskException.ErrorCode.INVALID_TASK, "title must not be blank.");
        String trimmedTitle = title.trim();
        if (trimmedTitle.length() > 120) {
            throw new TaskException(TaskException.ErrorCode.INVALID_TASK, "title must be 120 characters or fewer.");
        }
        String trimmedDesc = (description != null) ? description.trim() : "";
        if (trimmedDesc.length() > 500) {
            throw new TaskException(TaskException.ErrorCode.INVALID_TASK, "description must be 500 characters or fewer.");
        }
        this.id = id.trim();
        this.title = trimmedTitle;
        this.description = trimmedDesc;
        this.priority = (priority != null) ? priority : Priority.MEDIUM;
        this.completed = false;
        this.createdAt = (createdAt != null) ? createdAt : Instant.now();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTitle(String title) throws TaskException {
        if (title == null || title.isBlank()) {
            throw new TaskException(TaskException.ErrorCode.INVALID_TASK, "title must not be blank.");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 120) {
            throw new TaskException(TaskException.ErrorCode.INVALID_TASK, "title must be 120 characters or fewer.");
        }
        this.title = trimmed;
    }

    public void setDescription(String description) throws TaskException {
        String trimmed = (description != null) ? description.trim() : "";
        if (trimmed.length() > 500) {
            throw new TaskException(TaskException.ErrorCode.INVALID_TASK, "description must be 500 characters or fewer.");
        }
        this.description = trimmed;
    }

    public void setPriority(Priority priority) {
        this.priority = Objects.requireNonNull(priority, "priority");
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        String status = completed ? "X" : " ";
        return "[" + status + "] " + id + ": " + title + " (" + priority + ")"
                + (description.isEmpty() ? "" : " — " + description);
    }
}
