package persistence;

import exception.TaskException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import model.Priority;
import model.Task;

/**
 * JSON file implementation. Writes are atomic: serialize to {@code .tmp},
 * then {@code Files.move(ATOMIC_MOVE, REPLACE_EXISTING)}.
 *
 * <p>On startup, if the primary file is missing or corrupt, the loader
 * checks for a newer {@code .tmp} file and attempts recovery.
 */
public final class FileTaskRepository implements TaskRepository {
    private final Path file;

    // Advisory cross-process lock — held for the lifetime of this instance.
    private RandomAccessFile lockRaf;
    private FileLock fileLock;

    public FileTaskRepository(Path file) {
        this.file = file;
        tryAdvisoryLock();
    }

    /** Best-effort: warns if another instance holds the lock file. */
    private void tryAdvisoryLock() {
        try {
            Path lck = file.resolveSibling("tasks.lck");
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            lockRaf = new RandomAccessFile(lck.toFile(), "rw");
            FileChannel channel = lockRaf.getChannel();
            fileLock = channel.tryLock();
            if (fileLock == null) {
                System.err.println("[!] Warning: tasks.json may be in use by another instance. Proceed with caution.");
            }
        } catch (IOException e) {
            // Cannot check lock — proceed silently
        }
    }

    @Override
    public List<Task> load() throws IOException {
        Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");

        // Try the primary file first.
        if (Files.exists(file)) {
            try {
                return parseTasks(Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException | RuntimeException e) {
                // Primary file corrupt — try .tmp recovery if it exists.
                if (Files.exists(tmpFile)) {
                    try {
                        List<Task> recovered = parseTasks(Files.readString(tmpFile, StandardCharsets.UTF_8));
                        // Recovery succeeded — promote .tmp to primary.
                        Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING);
                        return recovered;
                    } catch (IOException | RuntimeException ignored) {
                        // Both files are bad; return empty.
                    }
                }
                // If .tmp doesn't exist or isn't usable, return empty.
                return List.of();
            } finally {
                // Clean up any stale .tmp after successful load attempt.
                try { Files.deleteIfExists(tmpFile); } catch (IOException ignored) { }
            }
        }

        // No primary file — maybe the .tmp is all we have.
        if (Files.exists(tmpFile)) {
            try {
                List<Task> recovered = parseTasks(Files.readString(tmpFile, StandardCharsets.UTF_8));
                Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return recovered;
            } catch (IOException | RuntimeException ignored) {
                try { Files.deleteIfExists(tmpFile); } catch (IOException ignored2) { }
            }
        }

        return List.of();
    }

    @Override
    public void save(List<Task> tasks) throws IOException {
        Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");
        String json = serializeTasks(tasks);

        // Ensure parent directory exists.
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }

        // Write to .tmp, then atomic move.
        Files.writeString(tmpFile, json, StandardCharsets.UTF_8);
        Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    // ---------------------------------------------------------------
    // JSON serialization (hand-rolled, schema-specific)
    // ---------------------------------------------------------------

    private static String serializeTasks(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 1,\n");
        sb.append("  \"tasks\": [\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": ").append(quote(t.getId())).append(",\n");
            sb.append("      \"title\": ").append(quote(t.getTitle())).append(",\n");
            sb.append("      \"description\": ").append(quote(t.getDescription() != null ? t.getDescription() : "")).append(",\n");
            sb.append("      \"priority\": ").append(quote(t.getPriority().name())).append(",\n");
            sb.append("      \"completed\": ").append(t.isCompleted()).append(",\n");
            sb.append("      \"createdAt\": ").append(quote(t.getCreatedAt().toString())).append("\n");
            sb.append("    }");
            if (i < tasks.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static List<Task> parseTasks(String json) throws IOException {
        // Reject content that is clearly not valid JSON.
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IOException("File does not contain valid JSON (missing braces)");
        }
        // Reject content without a "version" key — it's not our schema.
        if (!json.contains("\"version\"")) {
            throw new IOException("Missing \"version\" field — not a valid task store");
        }

        List<Task> tasks = new ArrayList<>();

        // Locate the "tasks" array.
        int tasksIdx = json.indexOf("\"tasks\"");
        if (tasksIdx < 0) return tasks;

        int arrayStart = json.indexOf('[', tasksIdx);
        if (arrayStart < 0) return tasks;

        // Walk through the array parsing each object block.
        int pos = arrayStart + 1;

        while (pos < json.length()) {
            // Skip whitespace and commas.
            char c = json.charAt(pos);
            if (c == ']') break;
            if (c == '{') {
                // Parse one task object.
                int end = findMatchingBrace(json, pos);
                if (end < 0) throw new IOException("Unmatched brace in tasks.json");

                String block = json.substring(pos, end + 1);
                Task task = parseTaskObject(block);
                if (task != null) {
                    tasks.add(task);
                }
                pos = end + 1;
            } else {
                pos++;
            }
        }

        return tasks;
    }

    private static int findMatchingBrace(String s, int start) {
        if (start >= s.length() || s.charAt(start) != '{') return -1;
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
            } else {
                if (c == '"') inString = true;
                else if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) return i; }
            }
        }
        return -1;
    }

    private static Task parseTaskObject(String block) throws IOException {
        try {
            String id = extractString(block, "id");
            String title = extractString(block, "title");
            String description = extractString(block, "description");
            String priorityStr = extractString(block, "priority");
            boolean completed = extractBoolean(block, "completed");
            String createdAtStr = extractString(block, "createdAt");

            if (id == null || title == null) return null;

            Priority priority = Priority.MEDIUM;
            if (priorityStr != null) {
                    try { priority = Priority.fromString(priorityStr); } catch (TaskException ignored) { }
            }

            Instant createdAt = Instant.now();
            if (createdAtStr != null && !createdAtStr.isEmpty()) {
                try { createdAt = Instant.parse(createdAtStr); } catch (Exception ignored) { }
            }

            Task task = new Task(id, title, description != null ? description : "", priority, createdAt);
            task.setCompleted(completed);
            return task;
        } catch (TaskException e) {
            throw new IOException("Failed to parse task: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Failed to parse task: " + e.getMessage(), e);
        }
    }

    private static String extractString(String block, String key) {
        String search = "\"" + key + "\":";
        int keyIdx = block.indexOf(search);
        if (keyIdx < 0) return null;

        int colonIdx = keyIdx + search.length();
        // Skip whitespace.
        while (colonIdx < block.length() && block.charAt(colonIdx) <= ' ') colonIdx++;
        if (colonIdx >= block.length() || block.charAt(colonIdx) != '"') return null;

        // Find closing quote, handling escapes.
        int start = colonIdx + 1;
        int end = start;
        while (end < block.length()) {
            if (block.charAt(end) == '\\') { end += 2; continue; }
            if (block.charAt(end) == '"') break;
            end++;
        }
        if (end >= block.length()) return null;

        String raw = block.substring(start, end);
        return unescape(raw);
    }

    private static boolean extractBoolean(String block, String key) {
        String search = "\"" + key + "\":";
        int keyIdx = block.indexOf(search);
        if (keyIdx < 0) return false;
        int valIdx = keyIdx + search.length();
        return block.substring(valIdx).trim().startsWith("true");
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
