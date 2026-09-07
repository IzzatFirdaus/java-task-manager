package cli;

import service.TaskManager;
import util.ConsolePrinter;
import util.InputReader;

/**
 * Read–eval–print loop. Translates stdin into {@link TaskManager} calls and
 * renders results via {@link ConsolePrinter}.
 */
@SuppressWarnings("unused") // fields/methods are wired during the implementation phase
public final class CommandLineInterface {
    private final TaskManager manager;
    private final InputReader input;
    private final ConsolePrinter printer;

    public CommandLineInterface(TaskManager manager, InputReader input, ConsolePrinter printer) {
        this.manager = manager;
        this.input = input;
        this.printer = printer;
    }

    public void run() {
        // STEP_7_IMPLEMENT: loop prompt -> read line -> parse verb -> dispatch.
        throw new UnsupportedOperationException("REPL not yet implemented");
    }

    // STEP_7_IMPLEMENT: private void dispatch(String[] tokens);
    // STEP_7_IMPLEMENT: private void handleAdd(String[] args);
    // STEP_7_IMPLEMENT: private void handleList(String[] args);
    // STEP_7_IMPLEMENT: private void handleComplete(String[] args);
    // STEP_7_IMPLEMENT: private void handleUncomplete(String[] args);
    // STEP_7_IMPLEMENT: private void handleUpdate(String[] args);
    // STEP_7_IMPLEMENT: private void handleDelete(String[] args);
    // STEP_7_IMPLEMENT: private void handleFind(String[] args);
    // STEP_7_IMPLEMENT: private void handleHelp();
}
