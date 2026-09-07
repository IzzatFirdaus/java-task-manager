# CLAUDE.md — CLI Quickstart

> One-page reference for LLM tools (Claude Code, Copilot CLI, Codex CLI, etc.) operating in this repo.

## Prerequisites
- JDK 17+ on `PATH`. Verify: `java -version`.

## Common Commands (PowerShell)

### Compile
```powershell
$files = Get-ChildItem -Recurse -Filter *.java src | Resolve-Path -Relative
javac -d bin -Xlint:all $files
```

### Run
```powershell
java -cp bin App
```

### Run tests (JUnit 5 console launcher)
```powershell
# One-time: download junit-platform-console-standalone into lib/
Invoke-WebRequest -Uri https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar -OutFile lib\junit-platform-console-standalone-1.10.2.jar

# Compile + run together (tests live alongside production code under src/):
$lib = "lib\junit-platform-console-standalone-1.10.2.jar"
$src = Get-ChildItem -Recurse -Filter *.java src | Resolve-Path -Relative
javac -d bin -Xlint:all -cp $lib $src
java -jar $lib --class-path bin --scan-class-path
```

### Clean build artifacts
```powershell
Remove-Item -Recurse -Force bin
```

### Reset persisted data
```powershell
Remove-Item -Force data\tasks.json
```

## IDE / Language Server Setup

The repo is intentionally metadata-free (no Maven, Gradle, `.classpath`, or `.project`).
When you open the project in VS Code + the Red Hat Java extension, JDT needs to know
where the source folder and the JUnit JAR are. A starter `.vscode/settings.json` is
checked in **for you, locally** (`.vscode/` is gitignored) with this content:

```json
{
    "java.project.sourcePaths": ["src"],
    "java.project.outputPath": "bin",
    "java.project.referencedLibraries": ["lib/**/*.jar"]
}
```

If JDT ever reports "Archive ... is not a valid ZIP" or "package does not match the
expected package" against the wrong paths, the language server is stale. Reload the
window (`Developer: Reload Window`) so it re-indexes against the current on-disk tree.

## Path Aliases
| Alias  | Path     | Purpose                       |
|--------|----------|-------------------------------|
| `SRC`  | `src/`   | Java sources + co-located JUnit tests |
| `BIN`  | `bin/`   | Compiled classes              |
| `DATA` | `data/`  | Runtime JSON store (gitignored) |
| `LIB`  | `lib/`   | Downloaded JARs (gitignored)  |
| `DOCS` | `./*.md` | PRD, ARCHITECTURE, AGENTS, CLAUDE |

## Repo Map
```
java-task-manager/
├── src/
│   ├── App.java                      # entry
│   ├── cli/                          # REPL (+ CommandLineInterfaceTest)
│   ├── exception/                    # domain errors
│   ├── model/                        # Task, Priority (+ tests)
│   ├── persistence/                  # repository (+ tests)
│   ├── service/                      # TaskManager (+ TaskManagerTest)
│   └── util/                         # I/O helpers
├── data/                             # runtime JSON
├── lib/                              # JUnit 5 console launcher (gitignored)
├── README.md
├── PRD.md
├── ARCHITECTURE.md
├── ARCHITECTURE-ESSENTIALS.md
├── AGENTS.md
└── CLAUDE.md
```

## In-CLI Verb Cheat Sheet
```
add <title>             # add task; prompts for description and priority
list                    # pending tasks, sorted by id
list --all              # include completed tasks
complete <id>           # mark task completed
uncomplete <id>         # mark task pending
update <id>             # edit title / description / priority
delete <id>             # remove task (with confirmation)
find <id>               # show one task
help                    # list verbs
exit                    # save and quit
```

## LLM Tool Tips
- Prefer `replace_string_in_file` for edits; use `insert_edit_into_file` for larger insertions.
- Never modify files under `bin/`, `data/`, or `lib/` (they are generated or runtime state).
- Before adding a dependency, re-read `PRD.md` NF1 — runtime deps are forbidden.
- Tests live next to the code they exercise (e.g. `src/service/TaskManagerTest.java`), not under a separate `test/` directory.
- When uncertain about a design call, add it to `ARCHITECTURE.md` §11 ("Open Questions") instead of guessing.
