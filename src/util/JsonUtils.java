package util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Priority;
import model.Task;

/**
 * Hand-rolled JSON serialization/deserialization for the API layer.
 * No third-party dependencies — uses only JDK 17+ APIs.
 *
 * <p>This is a minimal, schema-specific parser that handles the exact
 * JSON shapes used by the Task REST API and nothing more.
 */
public final class JsonUtils {

    private JsonUtils() {}

    // ---------------------------------------------------------------
    // Serialization (model → JSON)
    // ---------------------------------------------------------------

    /** Serialize a single {@link Task} to a JSON object string. */
    public static String toJson(Task task) {
        return toJsonObject(task, 0);
    }

    /** Serialize a list of tasks to a JSON array string. */
    public static String toJson(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append("  ").append(toJsonObject(tasks.get(i), 2));
            if (i < tasks.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /** Serialize an error payload: {@code {"error": "<message>"}}. */
    public static String toError(String message) {
        return "{\"error\": " + quote(message) + "}";
    }

    /** Serialize a success message: {@code {"message": "<msg>", "id": "<id>"}}. */
    public static String toSuccess(String message, String id) {
        return "{\"message\": " + quote(message) + ", \"id\": " + quote(id) + "}";
    }

    /** Serialize a generic message: {@code {"message": "<msg>"}}. */
    public static String toMessage(String message) {
        return "{\"message\": " + quote(message) + "}";
    }

    // ---------------------------------------------------------------
    // Deserialization (JSON → model fields)
    // ---------------------------------------------------------------

    /**
     * Parse a flat JSON object into key-value string pairs.
     * Handles string, number, boolean, and null values.
     * Nested objects and arrays are not supported — returns empty for those.
     */
    public static Map<String, String> parseObject(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return result;

        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return result;

        // Remove surrounding braces
        json = json.substring(1, json.length() - 1).trim();

        int pos = 0;
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == ',' || Character.isWhitespace(c)) { pos++; continue; }
            if (c != '"') break;

            // --- Parse key ---
            pos++; // skip opening quote
            String key = readJsonString(json, pos);
            pos += key.length() + 2; // key content + opening/closing quotes

            // --- Skip colon ---
            while (pos < json.length() && (json.charAt(pos) == ':' || Character.isWhitespace(json.charAt(pos)))) {
                pos++;
            }
            if (pos >= json.length()) break;

            // --- Parse value ---
            c = json.charAt(pos);
            if (c == '"') {
                // String value
                pos++; // skip opening quote
                String val = readJsonString(json, pos);
                pos += val.length() + 2;
                result.put(key, val);
            } else if (c == 't' || c == 'f') {
                // Boolean
                if (json.startsWith("true", pos)) {
                    result.put(key, "true");
                    pos += 4;
                } else if (json.startsWith("false", pos)) {
                    result.put(key, "false");
                    pos += 5;
                }
            } else if (c == 'n') {
                // null
                if (json.startsWith("null", pos)) {
                    result.put(key, null);
                    pos += 4;
                }
            } else if (c == '-' || c == '+' || Character.isDigit(c)) {
                // Number
                int start = pos;
                if (c == '-' || c == '+') pos++;
                while (pos < json.length() && (Character.isDigit(json.charAt(pos)) || json.charAt(pos) == '.' || json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
                    pos++;
                }
                result.put(key, json.substring(start, pos));
            } else if (c == '{' || c == '[') {
                // Skip nested objects/arrays
                int depth = 1;
                boolean inStr = false;
                pos++;
                while (pos < json.length() && depth > 0) {
                    char sc = json.charAt(pos);
                    if (inStr) {
                        if (sc == '\\') { pos += 2; continue; }
                        if (sc == '"') inStr = false;
                    } else {
                        if (sc == '"') inStr = true;
                        else if (sc == '{' || sc == '[') depth++;
                        else if (sc == '}' || sc == ']') depth--;
                    }
                    pos++;
                }
                result.put(key, null); // Not supported; mark as null
            } else {
                pos++;
            }
        }

        return result;
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private static final String INDENT = "  ";

    private static String toJsonObject(Task t, int indent) {
        String pad = INDENT.repeat(indent);
        String inner = INDENT.repeat(indent + 1);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(inner).append("\"id\": ").append(quote(t.getId())).append(",\n");
        sb.append(inner).append("\"title\": ").append(quote(t.getTitle())).append(",\n");
        sb.append(inner).append("\"description\": ").append(quote(t.getDescription() != null ? t.getDescription() : "")).append(",\n");
        sb.append(inner).append("\"priority\": ").append(quote(t.getPriority().name())).append(",\n");
        sb.append(inner).append("\"completed\": ").append(t.isCompleted()).append(",\n");
        sb.append(inner).append("\"createdAt\": ").append(quote(t.getCreatedAt().toString())).append("\n");
        sb.append(pad).append("}");
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

    /** Read a JSON string starting at {@code start} (character after opening quote). */
    private static String readJsonString(String s, int start) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                if (i + 1 < s.length()) {
                    char next = s.charAt(i + 1);
                    switch (next) {
                        case '"'  -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/'  -> sb.append('/');
                        case 'b'  -> sb.append('\b');
                        case 'f'  -> sb.append('\f');
                        case 'n'  -> sb.append('\n');
                        case 'r'  -> sb.append('\r');
                        case 't'  -> sb.append('\t');
                        case 'u'  -> { /* simplified: read 4 hex digits */
                            if (i + 5 < s.length()) {
                                sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                                i += 4;
                            }
                        }
                        default  -> sb.append(next);
                    }
                    i += 2;
                }
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /** Parse a priority string from JSON input, returning null for invalid values. */
    public static Priority parsePriority(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.toUpperCase().trim()) {
            case "LOW" -> Priority.LOW;
            case "MEDIUM" -> Priority.MEDIUM;
            case "HIGH" -> Priority.HIGH;
            default -> null;
        };
    }
}