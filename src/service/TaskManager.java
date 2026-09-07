package service;

import exception.DuplicateTaskException;
import exception.InvalidTaskException;
import exception.TaskNotFoundException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import model.Priority;
import model.Task;
import persistence.TaskRepository;
import util.IdGenerator;

/**
 * Domain layer. Pure business logic; no I/O.
 */
@SuppressWarnings("unused") // fields/methods are wired during the implementation phase
public class TaskManager {
    private final Map<String, Task> tasks = new LinkedHashMap<>();
    private final TaskRepository repository;
    private final IdGenerator ids;

    public TaskManager(TaskRepository repository, IdGenerator ids) {
        this.repository = repository;
        this.ids = ids;
        // STEP_7_IMPLEMENT: load existing tasks, seed ids from max(existing id) + 1.
    }

    public void addTask(Task task) throws DuplicateTaskException, InvalidTaskException {
        // STEP_7_IMPLEMENT: reject duplicate id, persist.
        throw new UnsupportedOperationException("addTask not yet implemented");
    }

    public void completeTask(String id) throws TaskNotFoundException {
        // STEP_7_IMPLEMENT: find, mark completed, persist.
        throw new UnsupportedOperationException("completeTask not yet implemented");
    }

    public void uncompleteTask(String id) throws TaskNotFoundException {
        // STEP_7_IMPLEMENT: find, mark pending, persist.
        throw new UnsupportedOperationException("uncompleteTask not yet implemented");
    }

    public void updateTask(String id, String title, String description, Priority priority)
            throws TaskNotFoundException, InvalidTaskException {
        // STEP_7_IMPLEMENT: find, mutate, persist.
        throw new UnsupportedOperationException("updateTask not yet implemented");
    }

    public void deleteTask(String id) throws TaskNotFoundException {
        // STEP_7_IMPLEMENT: find, remove, persist.
        throw new UnsupportedOperationException("deleteTask not yet implemented");
    }

    public Optional<Task> findTask(String id) {
        // STEP_7_IMPLEMENT: return Optional.ofNullable(tasks.get(id)).
        throw new UnsupportedOperationException("findTask not yet implemented");
    }

    public List<Task> listTasks(Predicate<Task> filter, Comparator<Task> sort) {
        // STEP_7_IMPLEMENT: filtered+sorted view of tasks.values().
        throw new UnsupportedOperationException("listTasks not yet implemented");
    }

    public String nextId() {
        // STEP_7_IMPLEMENT: delegate to IdGenerator.
        throw new UnsupportedOperationException("nextId not yet implemented");
    }
}
