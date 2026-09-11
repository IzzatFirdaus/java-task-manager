/**
 * Application entry point. Wires the REPL, service, repository, and helpers.
 * Supports an optional {@code --web} flag to start the HTTP API server.
 *
 * <p>Intentionally thin — all decisions live in the layers below.
 */
public final class App {
    private App() {}

    public static void main(String[] args) {
        persistence.FileTaskRepository repository =
                new persistence.FileTaskRepository(java.nio.file.Path.of("data", "tasks.json"));
        service.TaskManager manager = new service.TaskManager(repository);

        // Check for --web flag
        boolean webMode = false;
        boolean verbose = false;
        for (String arg : args) {
            if ("--web".equals(arg)) webMode = true;
            if ("--verbose".equals(arg) || "-v".equals(arg)) verbose = true;
        }

        if (webMode) {
            startWeb(manager, verbose);
        } else {
            startCli(manager);
        }
    }

    private static void startCli(service.TaskManager manager) {
        util.InputReader reader = new util.InputReader();
        util.ConsolePrinter printer = new util.ConsolePrinter();
        cli.CommandLineInterface cli = new cli.CommandLineInterface(manager, reader, printer);
        cli.run();
    }

    private static void startWeb(service.TaskManager manager, boolean verbose) {
        try {
            api.TaskHttpServer server = new api.TaskHttpServer(manager, verbose);
            server.start();
            System.out.println("Web interface: http://localhost:8080");
            System.out.println("Press Ctrl+C to stop.");
            // Keep the main thread alive
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Server interrupted.");
        } catch (java.io.IOException e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            System.exit(1);
        }
    }
}
