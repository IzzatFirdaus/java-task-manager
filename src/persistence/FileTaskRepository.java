package persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import model.Task;

/**
 * JSON file implementation. Writes are atomic: serialize to {@code .tmp},
 * then {@code Files.move(ATOMIC_MOVE, REPLACE_EXISTING)}.
 */
@SuppressWarnings("unused") // field is wired during the implementation phase
public final class FileTaskRepository implements TaskRepository {
    private final Path file;

    public FileTaskRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Task> load() throws IOException {
        // STEP_7_IMPLEMENT: read file if present, hand-roll parse JSON.
        throw new UnsupportedOperationException("FileTaskRepository.load not yet implemented");
    }

    @Override
    public void save(List<Task> tasks) throws IOException {
        // STEP_7_IMPLEMENT: write to tmp, atomic move, create parent dirs on demand.
        throw new UnsupportedOperationException("FileTaskRepository.save not yet implemented");
    }
}
