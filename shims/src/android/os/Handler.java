package android.os;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Desktop Handler: posts Runnables to {@link MainThread}. Messages are not used by the game. */
public class Handler {
    public interface Callback {
        boolean handleMessage(Message msg);
    }

    private final Set<Runnable> cancelled = Collections.newSetFromMap(new IdentityHashMap<>());

    public Handler() {}
    public Handler(Callback callback) {}
    public Handler(Looper looper) {}
    public Handler(Looper looper, Callback callback) {}

    public static Handler createAsync(Looper looper) { return new Handler(); }

    public void handleMessage(Message msg) {}

    public final boolean post(Runnable r) { return postDelayed(r, 0); }

    public final boolean postDelayed(Runnable r, long delayMillis) {
        synchronized (cancelled) { cancelled.remove(r); }
        MainThread.post(() -> {
            synchronized (cancelled) { if (cancelled.remove(r)) return; }
            r.run();
        }, delayMillis);
        return true;
    }

    public final boolean postDelayed(Runnable r, Object token, long delayMillis) { return postDelayed(r, delayMillis); }
    public final boolean postAtFrontOfQueue(Runnable r) { return post(r); }
    public final void removeCallbacks(Runnable r) { synchronized (cancelled) { cancelled.add(r); } }
    public final void removeCallbacks(Runnable r, Object token) { removeCallbacks(r); }
    public final void removeCallbacksAndMessages(Object token) {}
    public final Looper getLooper() { return Looper.getMainLooper(); }
}
