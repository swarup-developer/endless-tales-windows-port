package android.os;

/**
 * Desktop-only hook (not an Android class): decides where Handler/Looper work runs.
 * The game UI installs a scheduler that runs tasks on the JavaFX thread. Without one,
 * tasks run immediately on the calling thread, which is what headless tests want.
 */
public final class MainThread {
    public interface Scheduler {
        void post(Runnable r, long delayMillis);
        boolean isMainThread();
    }

    public static volatile Scheduler scheduler = null;

    private MainThread() {}

    public static void post(Runnable r, long delayMillis) {
        Scheduler s = scheduler;
        if (s == null) r.run();
        else s.post(r, Math.max(0, delayMillis));
    }
}
