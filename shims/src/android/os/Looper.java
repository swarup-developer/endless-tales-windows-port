package android.os;

/** Desktop Looper: there is one main looper, backed by {@link MainThread}. */
public final class Looper {
    private static final Looper MAIN = new Looper();

    Looper() {}

    public static void prepare() {}
    public static void prepareMainLooper() {}
    public static Looper getMainLooper() { return MAIN; }
    public static void loop() {}
    public static Looper myLooper() { return MAIN; }

    public boolean isCurrentThread() {
        MainThread.Scheduler s = MainThread.scheduler;
        return s == null || s.isMainThread();
    }

    public void quit() {}
    public void quitSafely() {}
    public Thread getThread() { return Thread.currentThread(); }
}
