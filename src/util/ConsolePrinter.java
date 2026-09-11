package util;

/** Formats output for the REPL. The only place allowed to write to stdout. */
public final class ConsolePrinter {
    public void printInfo(String line) {
        System.out.println("[*] " + line);
    }

    public void printError(String line) {
        System.err.println("[!] " + line);
    }

    public void printPrompt() {
        System.out.print("> ");
        System.out.flush();
    }
}
