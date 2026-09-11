package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import exception.TaskException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import model.Priority;
import model.Task;
import service.TaskManager;
import util.JsonUtils;

/**
 * Lightweight HTTP API server for the Task Manager.
 * Serves a REST API on {@code http://localhost:8080/api/tasks/...}
 * and optionally serves static frontend files from the {@code public/} directory.
 *
 * <p>Uses only {@code com.sun.net.httpserver.HttpServer} — no third-party dependencies.
 */
public class TaskHttpServer {

    private static final int PORT = 8080;
    private static final String API_PREFIX = "/api";
    private static final String TASKS_PREFIX = "/api/tasks";

    private final HttpServer server;
    private final TaskManager manager;
    private final boolean verbose;

    /**
     * Creates and starts the HTTP server.
     *
     * @param manager the task manager instance
     * @param verbose if true, prints request summaries to stderr
     * @throws IOException if the server cannot bind
     */
    public TaskHttpServer(TaskManager manager, boolean verbose) throws IOException {
        this.manager = manager;
        this.verbose = verbose;
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Register handlers
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);

        server.setExecutor(null); // use the default (daemon) executor
    }

    /** Start the server. */
    public void start() {
        server.start();
        System.err.println("[web] API server listening on http://localhost:" + PORT);
    }

    /** Stop the server gracefully (waits up to 1 second for active handlers). */
    public void stop() {
        server.stop(1);
        System.err.println("[web] API server stopped.");
    }

    // ---------------------------------------------------------------
    // API request router
    // ---------------------------------------------------------------

    private void handleApi(HttpExchange exchange) throws IOException {
        try {
            String method  = exchange.getRequestMethod().toUpperCase();
            URI uri        = exchange.getRequestURI();
            String path    = uri.getPath();
            String query   = uri.getQuery();

            // Set CORS headers on every API response
            setCorsHeaders(exchange);

            // Handle preflight
            if ("OPTIONS".equals(method)) {
                sendResponse(exchange, 204, "");
                return;
            }

            log("%s %s", method, path);

            // Parse path after /api/tasks
            String rest = path.substring(TASKS_PREFIX.length());

            if (rest.isEmpty() || rest.equals("/")) {
                // /api/tasks
                switch (method) {
                    case "GET"  -> handleListTasks(exchange, query);
                    case "POST" -> handleCreateTask(exchange);
                    default     -> sendMethodNotAllowed(exchange);
                }
            } else {
                // /api/tasks/{id} or /api/tasks/{id}/action
                String[] segments = rest.split("/", 3);
                // segments[0] is "" (leading slash), [1] is id, [2] is optional action
                if (segments.length < 2 || segments[1].isBlank()) {
                    sendNotFound(exchange, "Invalid path: " + path);
                    return;
                }
                String id = segments[1];
                String action = (segments.length > 2) ? segments[2] : null;

                if (action == null || action.isBlank()) {
                    // /api/tasks/{id}
                    switch (method) {
                        case "GET"    -> handleFindTask(exchange, id);
                        case "PUT"    -> handleUpdateTask(exchange, id);
                        case "DELETE" -> handleDeleteTask(exchange, id);
                        default       -> sendMethodNotAllowed(exchange);
                    }
                } else {
                    // /api/tasks/{id}/{action}
                    if (!"POST".equals(method)) {
                        sendMethodNotAllowed(exchange);
                        return;
                    }
                    switch (action) {
                        case "complete"   -> handleCompleteTask(exchange, id);
                        case "uncomplete" -> handleUncompleteTask(exchange, id);
                        default           -> sendNotFound(exchange, "Unknown action: " + action);
                    }
                }
            }
        } catch (Exception e) {
            log("ERROR: %s", e.getMessage());
            sendJson(exchange, 500, JsonUtils.toError("Internal server error: " + e.getMessage()));
        }
    }

    // ---------------------------------------------------------------
    // Static file server
    // ---------------------------------------------------------------

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Default to index.html
        if (path == null || path.equals("/")) {
            path = "/index.html";
        }

        // Security: reject paths with ".."
        if (path.contains("..")) {
            sendResponse(exchange, 403, "Forbidden");
            return;
        }

        // Resolve relative to public/ directory
        java.nio.file.Path filePath = java.nio.file.Path.of("public", path.substring(1));
        if (!java.nio.file.Files.exists(filePath)) {
            sendResponse(exchange, 404, "Not Found");
            return;
        }

        String contentType = detectContentType(path);
        byte[] content = java.nio.file.Files.readAllBytes(filePath);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        sendResponse(exchange, 200, new String(content, StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------
    // Handler implementations
    // ---------------------------------------------------------------

    private void handleListTasks(HttpExchange exchange, String query) throws IOException {
        Map<String, String> params = parseQuery(query);

        // Status filter: ?status=all or ?status=pending (default)
        Predicate<Task> filter;
        String status = params.getOrDefault("status", "pending");
        filter = switch (status.toLowerCase()) {
            case "all"     -> t -> true;
            case "pending" -> t -> !t.isCompleted();
            case "completed" -> t -> t.isCompleted();
            default        -> t -> !t.isCompleted();
        };

        // Sort: ?sort=id|priority|createdAt (default: id)
        Comparator<Task> sort;
        String sortBy = params.getOrDefault("sort", "id");
        sort = switch (sortBy.toLowerCase()) {
            case "priority"   -> Comparator.comparing(Task::getPriority).thenComparing(Task::getId);
            case "createdat"  -> Comparator.comparing(Task::getCreatedAt).thenComparing(Task::getId);
            default           -> Comparator.comparing(Task::getId);
        };

        List<Task> tasks = manager.listTasks(filter, sort);
        sendJson(exchange, 200, JsonUtils.toJson(tasks));
    }

    private void handleCreateTask(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> fields = JsonUtils.parseObject(body);

        String title = fields.get("title");
        if (title == null || title.isBlank()) {
            sendJson(exchange, 400, JsonUtils.toError("title is required and must not be blank."));
            return;
        }

        String description = fields.getOrDefault("description", "");
        Priority priority = JsonUtils.parsePriority(fields.get("priority"));
        if (priority == null) priority = Priority.MEDIUM;

        try {
            String id = manager.nextId();
            Task task = new Task(id, title, description, priority);
            manager.addTask(task);
            sendJson(exchange, 201, JsonUtils.toJson(task));
        } catch (TaskException e) {
            sendJson(exchange, 400, JsonUtils.toError(e.getMessage()));
        }
    }

    private void handleFindTask(HttpExchange exchange, String id) throws IOException {
        var opt = manager.findTask(id);
        if (opt.isPresent()) {
            sendJson(exchange, 200, JsonUtils.toJson(opt.get()));
        } else {
            sendJson(exchange, 404, JsonUtils.toError("Task " + id + " not found."));
        }
    }

    private void handleUpdateTask(HttpExchange exchange, String id) throws IOException {
        String body = readBody(exchange);
        Map<String, String> fields = JsonUtils.parseObject(body);

        // Verify task exists
        if (manager.findTask(id).isEmpty()) {
            sendJson(exchange, 404, JsonUtils.toError("Task " + id + " not found."));
            return;
        }

        try {
            String title = fields.get("title");       // null if not present
            String description = fields.get("description"); // null if not present
            Priority priority = JsonUtils.parsePriority(fields.get("priority"));

            manager.updateTask(id, title, description, priority);

            var updated = manager.findTask(id);
            if (updated.isPresent()) {
                sendJson(exchange, 200, JsonUtils.toJson(updated.get()));
            }
        } catch (TaskException e) {
            sendJson(exchange, 400, JsonUtils.toError(e.getMessage()));
        }
    }

    private void handleCompleteTask(HttpExchange exchange, String id) throws IOException {
        try {
            manager.completeTask(id);
            var task = manager.findTask(id);
            if (task.isPresent()) {
                sendJson(exchange, 200, JsonUtils.toJson(task.get()));
            }
        } catch (TaskException e) {
            sendJson(exchange, 404, JsonUtils.toError(e.getMessage()));
        }
    }

    private void handleUncompleteTask(HttpExchange exchange, String id) throws IOException {
        try {
            manager.uncompleteTask(id);
            var task = manager.findTask(id);
            if (task.isPresent()) {
                sendJson(exchange, 200, JsonUtils.toJson(task.get()));
            }
        } catch (TaskException e) {
            sendJson(exchange, 404, JsonUtils.toError(e.getMessage()));
        }
    }

    private void handleDeleteTask(HttpExchange exchange, String id) throws IOException {
        try {
            manager.deleteTask(id);
            sendJson(exchange, 200, JsonUtils.toMessage("Task " + id + " deleted."));
        } catch (TaskException e) {
            sendJson(exchange, 404, JsonUtils.toError(e.getMessage()));
        }
    }

    // ---------------------------------------------------------------
    // HTTP helpers
    // ---------------------------------------------------------------

    private void setCorsHeaders(HttpExchange exchange) {
        var headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        headers.set("Access-Control-Max-Age", "86400");
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        sendResponse(exchange, code, json);
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, JsonUtils.toError("Method not allowed"));
    }

    private void sendNotFound(HttpExchange exchange, String message) throws IOException {
        sendJson(exchange, 404, JsonUtils.toError(message));
    }

    /** Read the full request body into a UTF-8 string. */
    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    /** Parse a query string into a map. */
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new java.util.LinkedHashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(
                    java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                    java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8)
                );
            } else if (eq < 0 && !pair.isBlank()) {
                params.put(java.net.URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }

    private static String detectContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        if (path.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    private void log(String format, Object... args) {
        if (verbose) {
            System.err.printf("[web] " + format + "%n", args);
        }
    }
}