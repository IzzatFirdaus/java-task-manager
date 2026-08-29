package model;

public class Task {
    private String id;
    private String title;
    private boolean completed;

    public Task(String id, String title) {
        this.id = id;
        this.title = title;
        this.completed = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public boolean isCompleted() { return completed; }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return "[" + (completed ? "X" : " ") + "] " + id + ": " + title;
    }
}