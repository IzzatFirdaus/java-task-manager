package util;

/** Formats output for the REPL. The only place allowed to write to stdout. */
public final class ConsolePrinter {
    public void printInfo(String line) {
        // STEP_7_IMPLEMENT: System.out.println("[*] " + line);
        throw new UnsupportedOperationException("printInfo not yet implemented");
    }

    public void printError(String line) {
        // STEP_7_IMPLEMENT: System.err.println("[!] " + line);
        throw new UnsupportedOperationException("printError not yet implemented");
    }

    public void printPrompt() {
        // STEP_7_IMPLEMENT: System.out.print("> "); System.out.flush();
        throw new UnsupportedOperationException("printPrompt not yet implemented");
    }
}
