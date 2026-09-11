package service;

import exception.TaskException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import model.Priority;
import model.Task;
import persistence.TaskRepository;

/**
 * Domain layer. Pure business logic; no I/O.
 *
 * <p>All mutating operations persist to the repository after modifying
 * in-memory state. The constructor loads existing tasks from the
 * repository and seeds the ID generator from the highest existing id.
 */
public class TaskManager {
    private final Map<String, Task> tasks = new LinkedHashMap<>();
    private final TaskRepository repository;
    private final AtomicInteger nextId;

    public TaskManager(TaskRepository repository) {
        this.repository = repository;
        this.nextId = new AtomicInteger(1);
        loadFromRepository();
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    public void addTask(Task task) throws TaskException {
        if (task == null) throw new TaskException(TaskException.ErrorCode.INVALID_TASK, "task must not be null.");
        if (tasks.containsKey(task.getId())) {
            throw new TaskException(TaskException.ErrorCode.DUPLICATE_TASK, "Task " + task.getId() + " already exists.");
        }
        tasks.put(task.getId(), task);
        persist();
    }

    public void completeTask(String id) throws TaskException {
        Task task = findOrThrow(id);
        task.setCompleted(true);
        persist();
    }

    public void uncompleteTask(String id) throws TaskException {
        Task task = findOrThrow(id);
        task.setCompleted(false);
        persist();
    }

    public void updateTask(String id, String title, String description, Priority priority)
            throws TaskException {
        Task task = findOrThrow(id);
        if (title != null && !title.isBlank()) {
            task.setTitle(title);
        }
        // description: null/blank means "leave unchanged"
        if (description != null && !description.isBlank()) {
            task.setDescription(description);
        } else if (description != null && description.isBlank()) {
            // Explicit blank means "clear description"
            task.setDescription("");
        }
        if (priority != null) {
            task.setPriority(priority);
        }
        persist();
    }

    public void deleteTask(String id) throws TaskException {
        if (!tasks.containsKey(id)) {
            throw new TaskException(TaskException.ErrorCode.TASK_NOT_FOUND, "Task " + id + " not found.");
        }
        tasks.remove(id);
        persist();
    }

    public Optional<Task> findTask(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public List<Task> listTasks(Predicate<Task> filter, Comparator<Task> sort) {
        return tasks.values().stream()
                .filter(filter)
                .sorted(sort)
                .collect(Collectors.toList());
    }

    public String nextId() {
        return String.valueOf(nextId.getAndIncrement());
    }

    /** Returns an unmodifiable view of all tasks (for iteration). */
    public List<Task> tasks() {
        return List.copyOf(tasks.values());
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private void loadFromRepository() {
        try {
            List<Task> loaded = repository.load();
            for (Task t : loaded) {
                tasks.put(t.getId(), t);
            }
            int maxId = loaded.stream()
                    .mapToInt(t -> {
                        try { return Integer.parseInt(t.getId()); }
                        catch (NumberFormatException e) { return 0; }
                    })
                    .max()
                    .orElse(0);
            nextId.set(maxId + 1);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tasks from storage", e);
        }
    }

    private void persist() {
        try {
            repository.save(new ArrayList<>(tasks.values()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save tasks to storage", e);
        }
    }

    private Task findOrThrow(String id) throws TaskException {
        Task task = tasks.get(id);
        if (task == null) throw new TaskException(TaskException.ErrorCode.TASK_NOT_FOUND, "Task " + id + " not found.");
        return task;
    }
}
