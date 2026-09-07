package util;

import java.util.Optional;

/** Thin wrapper around stdin. */
public final class InputReader {
    private final java.io.BufferedReader reader;

    public InputReader() {
        this(new java.io.InputStreamReader(System.in));
    }

    public InputReader(java.io.Reader underlying) {
        this.reader = new java.io.BufferedReader(underlying);
    }

    public Optional<String> readLine() {
        try {
            String line = reader.readLine(); // STEP_7_IMPLEMENT: wrap in Optional.
            return Optional.ofNullable(line);
        } catch (java.io.IOException e) {
            return Optional.empty();
        }
    }

    public void close() {
        // STEP_7_IMPLEMENT: try { reader.close(); } catch (IOException ignored) {}
    }
}
