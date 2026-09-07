package persistence;

import java.io.IOException;
import java.util.List;
import model.Task;

/**
 * Persistence boundary for tasks. Implementations are expected to be safe to
 * call from a single thread (the REPL is the sole mutator).
 */
public interface TaskRepository {
    List<Task> load() throws IOException;

    void save(List<Task> tasks) throws IOException;
}
