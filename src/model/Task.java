package model;

import exception.InvalidTaskException;
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

    public Task(String id, String title, String description, Priority priority) throws InvalidTaskException {
        if (id == null || id.isBlank()) throw new InvalidTaskException("id must not be blank.");
        if (title == null || title.isBlank()) throw new InvalidTaskException("title must not be blank.");
        // STEP_7_IMPLEMENT: trim + length-check title (1..120), description (≤500).
        throw new UnsupportedOperationException("Task constructor not yet implemented");
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTitle(String title) throws InvalidTaskException {
        // STEP_7_IMPLEMENT: validate, then assign.
        throw new UnsupportedOperationException("setTitle not yet implemented");
    }

    public void setDescription(String description) throws InvalidTaskException {
        // STEP_7_IMPLEMENT: validate, then assign.
        throw new UnsupportedOperationException("setDescription not yet implemented");
    }

    public void setPriority(Priority priority) {
        this.priority = Objects.requireNonNull(priority, "priority");
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        // STEP_7_IMPLEMENT: format as [X] id: title (priority).
        throw new UnsupportedOperationException("Task.toString not yet implemented");
    }
}
