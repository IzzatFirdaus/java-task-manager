package cli;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumerates every CLI verb the REPL recognises. Keep the list in sync with
 * {@code CommandLineInterface#dispatch}.
 */
public enum Command {
    ADD, LIST, COMPLETE, UNCOMPLETE, UPDATE, DELETE, FIND, HELP, EXIT, UNKNOWN;

    public static Command fromToken(String token) {
        if (token == null) return UNKNOWN;
        return Arrays.stream(values())
                .filter(c -> c != UNKNOWN && c.name().equalsIgnoreCase(token))
                .findFirst()
                .or(() -> Optional.of(UNKNOWN))
                .get();
    }
}
