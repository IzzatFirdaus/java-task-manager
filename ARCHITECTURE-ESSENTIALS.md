# Architecture Essentials

> One-screen context for AI agents. For full design, read `ARCHITECTURE.md`. For product intent, read `PRD.md`.

## Non-Negotiable Rules
1. `model.Task.id` and `model.Task.createdAt` are immutable and unique within a `TaskManager` instance.
2. `service.TaskManager` performs no I/O. All reads and writes go through `TaskRepository`.
3. `cli` is the only package allowed to touch `System.in` / `System.out`. Domain layers stay silent. `api` may write to stderr for verbose logging.
4. Domain operations throw checked exceptions (`TaskNotFoundException`, `DuplicateTaskException`, `InvalidTaskException`). The CLI catches and prints; never swallow silently.
5. Storage file is `data/tasks.json`. Writes are atomic: serialize to `.tmp`, then `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`.
6. No third-party runtime dependencies. JDK 17+ only.
7. Hand-rolled JSON only — do not pull in Jackson or Gson.
8. Compile clean under `javac -Xlint:all`.
9. `api` depends on `service` and `model`; never on `cli`.

## Core Data Structures
- **`Task`** — immutable `id`, `createdAt`; mutable `title`, `description`, `priority`, `completed`.
- **`Priority`** — enum `{LOW, MEDIUM, HIGH}`; canonical parser `Priority.fromString`.
- **`TaskRepository`** — `load() : List<Task>` and `save(List<Task>) : void`.
- **`TaskManager` store** — `LinkedHashMap<String, Task>` (insertion order, O(1) lookup by id).
- **Tests** live next to production code under `src/`, mirroring each package (e.g. `service/TaskManagerTest.java` for `service.TaskManager`).

## Primary Routes (CLI verbs → service methods)
| Verb                 | Method                                                   |
|----------------------|----------------------------------------------------------|
| `add`                | `TaskManager.addTask(Task)`                              |
| `list`               | `TaskManager.listTasks(Predicate, Comparator)`           |
| `complete`           | `TaskManager.completeTask(String)`                       |
| `uncomplete`         | `TaskManager.uncompleteTask(String)`                     |
| `update`             | `TaskManager.updateTask(String, String, String, Priority)` |
| `delete`             | `TaskManager.deleteTask(String)`                         |
| `find`               | `TaskManager.findTask(String)` → `Optional<Task>`        |

## Component Wiring
```
App
 └── new CommandLineInterface(
        new TaskManager(new FileTaskRepository("data/tasks.json"),
                        new IdGenerator(seedFromExisting)),
        new InputReader(),
        new ConsolePrinter())
```

---

## Stress-Test Self-Audit

**What breaks first under load or edge cases?**
- `FileTaskRepository.save` rewrites the whole file on every mutation. At ~10k tasks the per-save cost grows linearly and will dominate CLI latency once `NF3` is approached. Mitigation paths: debounce writes, batch on graceful exit only, or migrate to append-only NDJSON. Out of scope for v1; flag in `ARCHITECTURE.md` if pursued.
- `IdGenerator` resets to 1 unless seeded from `max(existing ids) + 1` in the `TaskManager` constructor. This is easy to forget and will silently collide on the first `add` after a restart.
- Two CLI processes editing the same `data/tasks.json` will last-writer-win because there is no file lock. Out of scope but must stay documented.

**Failure modes / edge cases currently unaccounted for?**
- Disk full or permission denied on the atomic rename → raw `IOException` would leak to the user. CLI must wrap it as `Storage error: <message>` and continue without crashing.
- Clock skew between machines if `createdAt` ever becomes a sort key across hosts — not applicable today (single user, local file) but worth flagging if v2 adds sync.
- Future async mutation (e.g., a background reminder) would race with the REPL's synchronous updates. `LinkedHashMap` is not thread-safe; introduce explicit synchronization or an actor model before adding concurrency.

**What is over-engineered and can be simplified?**
- Hand-rolled JSON is ~80 LoC of parser/writer. It exists to honor NF1 (no runtime deps). Acceptable trade-off, but if NF1 is ever relaxed, replacing it with `java.util.Properties` (for small datasets) or a tiny embedded JSON lib is straightforward.
- Three separate exception classes could collapse into one `TaskException` with a `code` field. They are kept distinct because the CLI uses cheap `instanceof` dispatch and clearer Javadoc outweighs the extra file count.
- `IdGenerator` as a dedicated class is more ceremony than needed for a monotonic counter; it could become a private `int` field on `TaskManager` with a `synchronized nextId()` method. Revisit during simplification pass.
