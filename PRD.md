# PRD — Java Task Manager

## 1. Purpose
A lightweight, dependency-free console application that lets a single user capture, track, and complete tasks. It serves two audiences simultaneously: as a daily productivity tool and as a teaching artifact for Java OOP, collections, file I/O, and exception handling.

## 2. Target Audience
- **Primary:** Java learners building an OOP portfolio (the original audience of this repository).
- **Secondary:** Developers and students who want a fast, no-install personal task tracker they can run from any terminal with JDK 17+.

## 3. Personas
| Persona    | Goal                                      | Pain today                                                  |
|------------|-------------------------------------------|-------------------------------------------------------------|
| Learner    | Study clean OOP design end-to-end         | Tutorials hide real package structure and persistence        |
| Student    | Track assignments quickly in one place    | Heavy tools (Jira, Trello, Notion) are overkill              |
| Casual dev | Jot tasks without a GUI or account        | Browser-based task tools require sign-in and lose focus      |

## 4. Functional Requirements
- **F1.** Add a task with `id`, `title`, optional `description`, and `priority` (LOW, MEDIUM, HIGH).
- **F2.** List all tasks, with optional filter (`completed` / `pending`) and sort (`priority`, `createdAt`, `id`).
- **F3.** Mark a task complete by id; toggle back to pending with `uncomplete`.
- **F4.** Update a task's title, description, or priority by id.
- **F5.** Delete a task by id (with confirmation).
- **F6.** Find a task by id (returns one task or a "not found" error).
- **F7.** Persist tasks to disk between runs; load on startup, save after every mutation and on graceful exit.
- **F8.** Interactive CLI with verb-driven commands and clear, single-line errors on bad input.

## 5. Non-Functional Requirements
- **NF1.** Java SE 17+ only. No third-party runtime dependencies.
- **NF2.** Cold startup under 200 ms on a developer laptop.
- **NF3.** Stable behavior up to 10,000 tasks in memory.
- **NF4.** Clear, single-line error messages; never crash on user input.
- **NF5.** UTF-8 source files; cross-platform line endings.
- **NF6.** Code compiles cleanly under `javac -Xlint:all`.

## 6. Key User Flows
1. **Quick capture.** Launch app → `add` → enter title → see confirmation with assigned id.
2. **Daily review.** Launch app → `list` → `complete <id>` → `list --pending` → see what remains.
3. **Edit & clean up.** `update <id>` → adjust title/priority → `delete <id>` → confirm removed.
4. **Restart-safe.** Quit → relaunch → all prior tasks still present.

## 7. Edge Cases & Error Behaviors
- Empty title → reject with `Title must not be blank.`
- Duplicate id → reject with `Task <id> already exists.`
- Unknown id for `update` / `complete` / `uncomplete` / `delete` / `find` → reject with `Task <id> not found.`
- Corrupt or missing data file → log a warning, start with an empty list, allow a fresh save.
- Non-numeric or out-of-range priority value → reject and re-prompt.
- Ctrl+C / EOF on stdin → exit cleanly with `Goodbye.` and persist current state.

## 8. Success Metrics
- All CLI verbs execute without uncaught exceptions across the standard flows.
- Persistence survives process restart (verified by automated test).
- p95 command response time under 50 ms at 1,000 tasks.
- ≥ 80% line coverage on `service` and `persistence` packages.
