package util;

import java.io.UncheckedIOException;
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

    /**
     * Reads a line from stdin.
     *
     * @return the line content, or {@link Optional#empty()} on EOF
     * @throws UncheckedIOException if an I/O error occurs (distinct from EOF)
     */
    public Optional<String> readLine() {
        try {
            String line = reader.readLine();
            return Optional.ofNullable(line);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("Failed to read from stdin", e);
        }
    }

    public void close() {
        try {
            reader.close();
        } catch (java.io.IOException ignored) {
            // closing stdin — nothing to recover
        }
    }
}
