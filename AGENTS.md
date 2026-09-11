# AGENTS.md — Operational Rules

> Rules for AI agents and human contributors working in `java-task-manager/`. Read `PRD.md`, `ARCHITECTURE.md`, and `ARCHITECTURE-ESSENTIALS.md` before touching code.

## 1. Safety Rules (do not violate)
1. **Never edit compiled output.** Files under `bin/` are generated; modify `src/` only.
2. **Never edit `data/tasks.json` by hand.** It is runtime state. Regenerate by deleting the file or running the app.
3. **Never introduce runtime dependencies** without an explicit user request. The project is dependency-free by design (`PRD.md` NF1).
4. **Never swallow exceptions silently.** Catch only at the CLI boundary; otherwise rethrow or log.
5. **Never call `System.out` / `System.in` outside `cli/` and `util/`.** All other layers must stay silent.
6. **Never bypass the layering rule.** `service` depends on `model` and `persistence`; `cli` depends on `service`, `model`, `util`, `exception`. Reverse dependencies are forbidden. `api` depends on `service` and `model` but NOT on `cli`.

## 2. Coding Standards
- Java 17 features permitted: records, sealed types, switch expressions, `var` in local scope, `List.of`, `Map.of`, `Files.readString`, `Instant`.
- Indentation: 4 spaces. No tabs. No trailing whitespace.
- One public top-level type per file; filename must match.
- Package layout: see `ARCHITECTURE.md` §2. Do not invent new top-level packages.
- Imports: no wildcard imports. Group as `java.*`, `javax.*`, third-party, local — separated by blank lines.
- Line length target 120, hard cap 140.
- Javadoc on every public type and every public method that is part of the CLI or service surface.

## 3. Preferred Design Patterns
- **Repository** for persistence (`persistence.TaskRepository`).
- **REPL / verb dispatch** for CLI: `Command` enum → `CommandLineInterface.run` switch.
- **Checked exceptions at domain boundaries**, swallowed only at the CLI.
- **Immutable identity + mutable state** on `Task` — id identifies forever; content evolves.

## 4. Required Workflow for Feature Implementation
1. **Read** `PRD.md`, `ARCHITECTURE.md`, `ARCHITECTURE-ESSENTIALS.md`, `AGENTS.md`.
2. **Update `ARCHITECTURE.md` first** if the change affects data model, packages, CLI surface, or storage schema.
3. **Write tests** next to the code they exercise, mirroring the package (`service.TaskManagerTest` for `service.TaskManager`).
4. **Implement bottom-up:** `model` → `persistence` → `service` → `cli` → `App`.
5. **Compile** with `javac -Xlint:all`; resolve every warning before claiming done.
6. **Run** the affected CLI verb(s) end-to-end.
7. **Update `README.md`** only if the user-facing flow changed.

## 5. Anti-Patterns to Reject
- Adding Maven or Gradle "to make it easier."
- Pulling in Jackson, Gson, or any JSON library.
- Mixing I/O and domain logic inside `TaskManager`.
- Returning `null` instead of `Optional` from lookups.
- Logging via `System.err.println` from non-CLI layers.
- Mutating `Task.id` or `Task.createdAt`.
- Catching `Throwable` or `Exception` at the service layer.

## 6. Commit Hygiene
- One logical change per commit.
- Commit message format: `<scope>: <imperative summary>` (e.g., `service: validate title length`).
- Do not commit `bin/`, `data/tasks.json`, or anything matched by `.gitignore`.

## 7. When You Are Unsure
- Default to the simplest design that satisfies the PRD.
- Surface trade-offs in `ARCHITECTURE.md` §11 ("Open Questions") rather than inventing a new rule.
- Ask the user only when a decision is irreversible (schema break, dependency addition, public CLI change).
