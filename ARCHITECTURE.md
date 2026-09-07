# ARCHITECTURE.md — Java Task Manager

> Source of truth for technical design. Product intent lives in `PRD.md`; the condensed rule set lives in `ARCHITECTURE-ESSENTIALS.md`.

## 1. Tech Stack
- **Language:** Java SE 17. Records, sealed types, switch expressions, `List.of`, `Files.readString`, `Instant` are all fair game.
- **Build:** Plain `javac` → `bin/`. No Maven, no Gradle.
- **Runtime deps:** None. The shipped app loads only `java.*`.
- **Test deps:** JUnit 5 (console launcher) lives in `lib/`. Tests are co-located with production code under `src/` mirroring each package (e.g. `service/TaskManagerTest.java`); they are not on the runtime classpath.
- **Storage:** JSON file at `data/tasks.json`. Hand-rolled serialization to honor NF1.
- **Encoding:** UTF-8 source; LF or CRLF line endings tolerated.

## 2. Package / Module Layout
```
src/
├── App.java                       # entry point, wires CLI + service + repository
├── cli/
│   ├── Command.java               # enum of supported verbs
│   └── CommandLineInterface.java  # REPL: stdin -> dispatch -> stdout
├── exception/
│   ├── DuplicateTaskException.java
│   ├── InvalidTaskException.java
│   └── TaskNotFoundException.java
├── model/
│   ├── Priority.java              # enum LOW, MEDIUM, HIGH
│   └── Task.java                  # immutable id/createdAt; mutable title/description/priority/completed
├── persistence/
│   ├── TaskRepository.java        # interface
│   └── FileTaskRepository.java    # JSON file implementation
├── service/
│   └── TaskManager.java           # domain operations, no I/O
└── util/
    ├── ConsolePrinter.java        # formatted output
    ├── IdGenerator.java           # monotonic id source
    └── InputReader.java           # stdin wrapper
├── (mirror packages)              # JUnit 5 tests, e.g. service/TaskManagerTest.java
data/
└── tasks.json                     # runtime store; .gitignored
```

Layering rule: `model` ← `service` ← `cli`. `persistence` and `util` are siblings to `service`; `cli` may depend on all.

## 3. Data Model

### 3.1 `Task`
| Field         | Type       | Notes                                       |
|---------------|------------|---------------------------------------------|
| `id`          | `String`   | immutable, unique, assigned on create       |
| `title`       | `String`   | required, trimmed, 1..120 chars             |
| `description` | `String`   | optional, ≤ 500 chars                       |
| `priority`    | `Priority` | default `MEDIUM`                            |
| `completed`   | `boolean`  | default `false`                             |
| `createdAt`   | `Instant`  | immutable, UTC                              |

### 3.2 `Priority`
```java
public enum Priority { LOW, MEDIUM, HIGH }
```
`Priority.fromString(String)` is the canonical parser; returns `MEDIUM` on null/blank input and throws `InvalidTaskException` on unknown values.

### 3.3 Storage schema (`data/tasks.json`)
```json
{
  "version": 1,
  "tasks": [
    {
      "id": "1",
      "title": "Setup JDK",
      "description": "",
      "priority": "MEDIUM",
      "completed": false,
      "createdAt": "2026-09-07T10:00:00Z"
    }
  ]
}
```
Top-level `version` enables future migrations.

## 4. Component Responsibilities

### 4.1 `service.TaskManager`
Pure domain logic. No I/O, no `System.out`. Holds a `TaskRepository` reference plus an in-memory map.
- `addTask(Task)` → throws `DuplicateTaskException` / `InvalidTaskException`.
- `completeTask(String id)` → throws `TaskNotFoundException`.
- `uncompleteTask(String id)` → throws `TaskNotFoundException`.
- `updateTask(String id, String title, String description, Priority priority)` → throws `TaskNotFoundException` / `InvalidTaskException`.
- `deleteTask(String id)` → throws `TaskNotFoundException`.
- `findTask(String id)` → returns `Optional<Task>`.
- `listTasks(Predicate<Task> filter, Comparator<Task> sort)` → `List<Task>`.
- `tasks()` → unmodifiable view for iteration.

### 4.2 `persistence.TaskRepository`
```java
public interface TaskRepository {
    List<Task> load() throws IOException;
    void save(List<Task> tasks) throws IOException;
}
```
- `FileTaskRepository` reads/writes `data/tasks.json`.
- Creates parent dirs on first save.
- Atomic write: serialize to `tasks.json.tmp`, then `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`.
- Missing file → returns empty list.
- Parse error → throws `IOException`; CLI catches and warns.

### 4.3 `cli.CommandLineInterface`
REPL loop:
1. Print prompt `> `.
2. Read line, split into `<verb> [args...]`.
3. Resolve `Command` enum.
4. Dispatch to the matching `TaskManager` method.
5. Print result via `ConsolePrinter` or a one-line error.
6. On `exit` / EOF: save and quit.

Supported verbs: `add`, `list`, `complete`, `uncomplete`, `update`, `delete`, `find`, `help`, `exit`.

### 4.4 `util.IdGenerator`
```java
public final class IdGenerator {
    private int next;
    public IdGenerator(int seed) { this.next = seed; }
    public synchronized String nextId() { return String.valueOf(next++); }
}
```
Seeded from `max(existing ids) + 1` at `TaskManager` construction.

## 5. CLI Command Surface

| Verb         | Args                                            | Behavior                                           |
|--------------|-------------------------------------------------|----------------------------------------------------|
| `add`        | `<title>`                                       | Prompts for description and priority               |
| `list`       | `[--all\|--pending] [--sort=priority\|createdAt\|id]` | Default: pending, sort by id                |
| `complete`   | `<id>`                                          | Marks task completed                               |
| `uncomplete` | `<id>`                                          | Marks task pending                                 |
| `update`     | `<id>`                                          | Interactive prompts for title / description / priority |
| `delete`     | `<id>`                                          | Confirms then removes                              |
| `find`       | `<id>`                                          | Prints a single task                               |
| `help`       | —                                               | Lists verbs                                        |
| `exit`       | —                                               | Saves and quits                                    |

## 6. Error Model
Checked exceptions thrown by `service`:
- `TaskNotFoundException`
- `DuplicateTaskException`
- `InvalidTaskException`

The CLI catches each, prints a one-line message via `ConsolePrinter.printError`, and continues the loop. `IOException` from the repository is caught at the same boundary and reported as `Storage error: <message>`.

## 7. Threading & Concurrency
Single-threaded by design. `TaskManager` is the sole mutator of the in-memory map. `IdGenerator` is `synchronized` because its counter is mutable; no other shared state.

## 8. Environment & Configuration
- `JAVA_HOME` ≥ 17 on `PATH`.
- Working directory must contain a writable `data/` folder (auto-created on first save).
- No environment variables required.

## 9. Build & Run (technical pin)
```powershell
$files = Get-ChildItem -Recurse -Filter *.java src | Resolve-Path -Relative
javac -d bin -Xlint:all $files
java -cp bin App
```

## 10. Testing Strategy
- **Unit:** `TaskTest`, `TaskManagerTest`, `PriorityTest`, `FileTaskRepositoryTest` (temp dir).
- **Integration:** CLI test driving stdin via `System.setIn(BufferedReader)` and asserting on stdout.
- **Coverage target:** `service` and `persistence` ≥ 80% lines.

## 11. Open Questions / Out of Scope (v1)
- Multi-user file locking.
- Recurring tasks, due dates, tags.
- Network sync, web UI.
- Configurable storage path.
