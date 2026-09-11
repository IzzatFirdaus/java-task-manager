package cli;

import exception.TaskException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import model.Priority;
import model.Task;
import service.TaskManager;
import util.ConsolePrinter;
import util.InputReader;

/**
 * Read–eval–print loop. Translates stdin into {@link TaskManager} calls and
 * renders results via {@link ConsolePrinter}.
 */
public final class CommandLineInterface {
    private final TaskManager manager;
    private final InputReader input;
    private final ConsolePrinter printer;

    public CommandLineInterface(TaskManager manager, InputReader input, ConsolePrinter printer) {
        this.manager = manager;
        this.input = input;
        this.printer = printer;
    }

    /** Runs the REPL loop until the user exits or sends EOF. */
    public void run() {
        printer.printInfo("Java Task Manager — type 'help' for commands, 'exit' to quit.");

        while (true) {
            printer.printPrompt();
            Optional<String> lineOpt;
            try {
                lineOpt = input.readLine();
            } catch (UncheckedIOException e) {
                printer.printError("Read error: " + e.getCause().getMessage());
                continue;
            }

            // EOF
            if (lineOpt.isEmpty()) {
                printer.printInfo("Goodbye.");
                return;
            }

            String line = lineOpt.get().trim();
            if (line.isEmpty()) continue;

            try {
                String[] tokens = tokenize(line);
                dispatch(tokens);
            } catch (RuntimeException e) {
                // Catch runtime failures from the I/O wrapper in TaskManager.
                printer.printError("Storage error: " + e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // Dispatch
    // ---------------------------------------------------------------

    private void dispatch(String[] tokens) {
        Command cmd = Command.fromToken(tokens[0]);
        String[] args = new String[tokens.length - 1];
        if (args.length > 0) System.arraycopy(tokens, 1, args, 0, args.length);

        try {
            switch (cmd) {
                case ADD       -> handleAdd(args);
                case LIST      -> handleList(args);
                case COMPLETE  -> handleComplete(args);
                case UNCOMPLETE -> handleUncomplete(args);
                case UPDATE    -> handleUpdate(args);
                case DELETE    -> handleDelete(args);
                case FIND      -> handleFind(args);
                case HELP      -> handleHelp();
                case EXIT      -> handleExit();
                case UNKNOWN   -> printer.printError("Unknown command. Type 'help' for available commands.");
            }
        } catch (UncheckedIOException e) {
            printer.printError("Read error: " + e.getCause().getMessage());
        } catch (TaskException e) {
            printer.printError(e.getMessage());
        } catch (RuntimeException e) {
            printer.printError("Storage error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    // ---------------------------------------------------------------
    // Argument helpers
    // ---------------------------------------------------------------

    /** Validates that args[0] is a present, non-blank, numeric task ID; returns trimmed ID or null. */
    private static String validatedId(String[] args) {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) return null;
        String trimmed = args[0].trim();
        if (!trimmed.matches("\\d+")) return null;
        return trimmed;
    }

    // ---------------------------------------------------------------
    // Command handlers
    // ---------------------------------------------------------------

    private void handleAdd(String[] args) {
        if (args.length == 0) {
            printer.printError("Usage: add <title>");
            return;
        }

        String title = args[0];
        if (args.length > 1) {
            // Rejoin remaining tokens as part of the title (quoted-string support).
            StringBuilder sb = new StringBuilder(title);
            for (int i = 1; i < args.length; i++) {
                sb.append(" ").append(args[i]);
            }
            title = sb.toString();
        }

        try {
            // Prompt for description.
            printer.printInfo("Enter description (optional, press Enter to skip):");
            printer.printPrompt();
            Optional<String> descOpt = input.readLine();
            String description = descOpt.orElse("");

            // Prompt for priority.
            Priority priority = promptPriority();

            String id = manager.nextId();
            Task task = new Task(id, title, description, priority);
            manager.addTask(task);
            printer.printInfo("Task " + id + " added: " + title);
        } catch (TaskException e) {
            printer.printError(e.getMessage());
        }
    }

    private void handleList(String[] args) {
        boolean showAll = false;
        String sortBy = "id";

        for (String arg : args) {
            if ("--all".equals(arg) || "-a".equals(arg)) {
                showAll = true;
            } else if ("--pending".equals(arg) || "-p".equals(arg)) {
                showAll = false;
            } else if (arg.startsWith("--sort=")) {
                sortBy = arg.substring(7);
            }
        }

        Comparator<Task> comparator = switch (sortBy) {
            case "priority" -> Comparator.comparing(Task::getPriority).thenComparing(Task::getId);
            case "createdAt" -> Comparator.comparing(Task::getCreatedAt);
            default -> Comparator.comparing(Task::getId);
        };

        List<Task> result = showAll
                ? manager.listTasks(t -> true, comparator)
                : manager.listTasks(t -> !t.isCompleted(), comparator);

        if (result.isEmpty()) {
            printer.printInfo(showAll ? "No tasks." : "No pending tasks.");
            return;
        }

        for (Task task : result) {
            printer.printInfo(task.toString());
        }
    }

    private void handleComplete(String[] args) throws TaskException {
        String id = validatedId(args);
        if (id == null) {
            printer.printError("Usage: complete <id>");
            return;
        }
        manager.completeTask(id);
        printer.printInfo("Task " + id + " marked as completed.");
    }

    private void handleUncomplete(String[] args) throws TaskException {
        String id = validatedId(args);
        if (id == null) {
            printer.printError("Usage: uncomplete <id>");
            return;
        }
        manager.uncompleteTask(id);
        printer.printInfo("Task " + id + " marked as pending.");
    }

    private void handleUpdate(String[] args) throws TaskException {
        String id = validatedId(args);
        if (id == null) {
            printer.printError("Usage: update <id>");
            return;
        }

        Optional<Task> existing = manager.findTask(id);
        if (existing.isEmpty()) {
            throw new TaskException(TaskException.ErrorCode.TASK_NOT_FOUND, "Task " + id + " not found.");
        }

        Task current = existing.get();

        // Prompt for new title (blank = keep).
        printer.printInfo("Enter new title (press Enter to keep: \"" + current.getTitle() + "\"):");
        printer.printPrompt();
        Optional<String> titleOpt = input.readLine();
        String title = titleOpt.filter(s -> !s.isBlank()).orElse(null);

        // Prompt for new description (blank = keep; whitespace-only = clear).
        printer.printInfo("Enter new description (press Enter to keep, \" \" to clear):");
        printer.printPrompt();
        Optional<String> descOpt = input.readLine();
        String description;
        if (descOpt.isEmpty()) {
            description = null; // EOF → keep unchanged
        } else {
            String raw = descOpt.get();
            if (raw.isEmpty()) {
                description = null; // Enter → keep unchanged
            } else if (raw.trim().isEmpty()) {
                description = ""; // whitespace-only → clear
            } else {
                description = raw.trim();
            }
        }

        // Prompt for priority.
        printer.printInfo("Enter new priority (LOW / MEDIUM / HIGH, press Enter to keep \"" + current.getPriority() + "\"):");
        Priority priority = null;
        while (true) {
            printer.printPrompt();
            Optional<String> prioOpt = input.readLine();
            if (prioOpt.isEmpty() || prioOpt.get().isBlank()) break; // keep existing
            try {
                priority = Priority.fromString(prioOpt.get());
                break;
            } catch (TaskException e) {
                printer.printError(e.getMessage() + " Try again.");
            }
        }

        try {
            manager.updateTask(id, title, description, priority);
            printer.printInfo("Task " + id + " updated.");
        } catch (TaskException e) {
            printer.printError(e.getMessage());
        }
    }

    private void handleDelete(String[] args) throws TaskException {
        String id = validatedId(args);
        if (id == null) {
            printer.printError("Usage: delete <id>");
            return;
        }

        Optional<Task> existing = manager.findTask(id);
        if (existing.isEmpty()) {
            throw new TaskException(TaskException.ErrorCode.TASK_NOT_FOUND, "Task " + id + " not found.");
        }

        printer.printInfo("Are you sure you want to delete task " + id + " (\"" + existing.get().getTitle() + "\")? [y/N]:");
        printer.printPrompt();
        Optional<String> confirm = input.readLine();

        if (confirm.isEmpty()) {
            printer.printInfo("Cancelled.");
            return;
        }

        String response = confirm.get().trim().toLowerCase();
        if ("y".equals(response) || "yes".equals(response)) {
            manager.deleteTask(id);
            printer.printInfo("Task " + id + " deleted.");
        } else {
            printer.printInfo("Cancelled.");
        }
    }

    private void handleFind(String[] args) {
        String id = validatedId(args);
        if (id == null) {
            printer.printError("Usage: find <id>");
            return;
        }

        Optional<Task> task = manager.findTask(id);
        if (task.isPresent()) {
            printer.printInfo(task.get().toString());
        } else {
            printer.printError("Task " + id + " not found.");
        }
    }

    private void handleHelp() {
        printer.printInfo("Available commands:");
        printer.printInfo("  add <title>          — Add a new task (prompts for description & priority)");
        printer.printInfo("  list [--all] [--sort=priority|createdAt|id] — List tasks");
        printer.printInfo("  complete <id>        — Mark task as completed");
        printer.printInfo("  uncomplete <id>      — Mark task as pending");
        printer.printInfo("  update <id>          — Edit task fields");
        printer.printInfo("  delete <id>          — Remove task (with confirmation)");
        printer.printInfo("  find <id>            — Show one task");
        printer.printInfo("  help                 — Show this help");
        printer.printInfo("  exit                 — Save and quit");
    }

    private void handleExit() {
        printer.printInfo("Goodbye.");
        input.close();
        // Exit the JVM to break the REPL loop. Tasks are already persisted
        // after every mutation via TaskManager.
        System.exit(0);
    }

    // ---------------------------------------------------------------
    // Prompt helpers
    // ---------------------------------------------------------------

    private Priority promptPriority() {
        printer.printInfo("Enter priority (LOW / MEDIUM / HIGH, press Enter for MEDIUM):");
        while (true) {
            printer.printPrompt();
            Optional<String> line = input.readLine();
            if (line.isEmpty() || line.get().isBlank()) return Priority.MEDIUM;
            try {
                return Priority.fromString(line.get());
            } catch (TaskException e) {
                printer.printError(e.getMessage() + " Try again.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Simple tokenizer supporting double-quoted strings
    // ---------------------------------------------------------------

    private static String[] tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens.toArray(String[]::new);
    }
}
