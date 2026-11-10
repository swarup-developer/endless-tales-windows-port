package android.os;

/** Desktop SystemClock. */
public final class SystemClock {
    private static final long START = System.nanoTime();

    private SystemClock() {}

    public static long elapsedRealtime() { return (System.nanoTime() - START) / 1_000_000L; }
    public static long elapsedRealtimeNanos() { return System.nanoTime() - START; }
    public static long uptimeMillis() { return elapsedRealtime(); }
    public static long currentThreadTimeMillis() { return elapsedRealtime(); }
    public static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } }
    public static boolean setCurrentTimeMillis(long millis) { return false; }
}
