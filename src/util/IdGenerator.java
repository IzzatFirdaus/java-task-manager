package util;

/**
 * Monotonic id source. Seeded above the largest existing id so restarts never
 * collide.
 */
@SuppressWarnings("unused") // field is wired during the implementation phase
public final class IdGenerator {
    private int next;

    public IdGenerator(int seed) {
        if (seed < 1) throw new IllegalArgumentException("seed must be >= 1");
        this.next = seed;
    }

    public synchronized String nextId() {
        // STEP_7_IMPLEMENT: return String.valueOf(next++);
        throw new UnsupportedOperationException("IdGenerator.nextId not yet implemented");
    }
}
